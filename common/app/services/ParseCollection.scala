package services

import common.FaciaMetrics.S3AuthorizationError
import common._
import conf.LiveContentApi
import conf.Configuration
import model._
import play.api.libs.json.Json._
import play.api.libs.json._
import scala.concurrent.Future
import contentapi.{ContentApiClient, QueryDefaults}
import scala.util.Try
import org.joda.time.DateTime
import performance._
import org.apache.commons.codec.digest.DigestUtils._
import ParseCollectionJsonImplicits._
import com.gu.openplatform.contentapi.model.{Content => ApiContent}
import play.api.libs.ws.Response
import play.api.libs.json.JsObject
import scala.concurrent.duration._
import conf.Switches.{FaciaToolCachedContentApiSwitch, FaciaToolCachedZippingContentApiSwitch}


object Path {
  def unapply[T](uri: String) = Some(uri.split('?')(0))
  def apply[T](uri: String) = uri.split('?')(0)
}

object Seg {
  def unapply(path: String): Option[List[String]] = path.split("/").toList match {
    case "" :: rest => Some(rest)
    case all => Some(all)
  }
}

//Curated and editorsPicks are the same, we will get rid of either
case class Result(
                   curated: List[ApiContent],
                   editorsPicks: List[ApiContent],
                   mostViewed: List[ApiContent],
                   contentApiResults: List[ApiContent]
                   )

object Result {
  val empty: Result = Result(Nil, Nil, Nil, Nil)
}

trait ParseCollection extends ExecutionContexts with QueryDefaults with Logging {
  implicit def apiContentCodec =
    if (FaciaToolCachedZippingContentApiSwitch.isSwitchedOn)
      JsonCodecs.snappyCodec[Option[ApiContent]]
    else
      JsonCodecs.nonGzippedCodec[Option[ApiContent]]

  implicit def resultCodec =
    if (FaciaToolCachedZippingContentApiSwitch.isSwitchedOn)
      JsonCodecs.snappyCodec[Result]
    else
      JsonCodecs.nonGzippedCodec[Result]

  val cacheDuration: FiniteDuration = 5.minutes

  val client: ContentApiClient
  def retrieveItemsFromCollectionJson(collectionJson: JsValue): Seq[CollectionItem]

  case class InvalidContent(id: String) extends Exception(s"Invalid Content: $id")
  val showFieldsQuery: String = FaciaDefaults.showFields
  val showFieldsWithBodyQuery: String = FaciaDefaults.showFieldsWithBody

  case class CollectionMeta(
    lastUpdated: Option[String],
    updatedBy: Option[String],
    updatedEmail: Option[String],
    displayName: Option[String],
    href: Option[String])
  object CollectionMeta {
    def empty: CollectionMeta = CollectionMeta(None, None, None, None, None)
  }

  case class CollectionItem(id: String, metaData: Option[Map[String, JsValue]], webPublicationDate: Option[DateTime]) {
    val isSnap: Boolean = id.startsWith("snap/")
  }

  def requestCollection(id: String): Future[Response] = {
    val s3BucketLocation: String = s"${S3FrontsApi.location}/collection/$id/collection.json"
    log.info(s"loading running order configuration from: ${Configuration.frontend.store}/$s3BucketLocation")
    val request = SecureS3Request.urlGet(s3BucketLocation)
    request.withRequestTimeout(2000).get()
  }

  def getCollection(id: String, config: Config, edition: Edition): Future[Collection] = {
    val collectionJson: Future[JsValue] = requestCollection(id)
      .map(responseToJson)

    val curatedItems: Future[List[Content]] = collectionJson
        .map(retrieveItemsFromCollectionJson)
        .flatMap { items => getArticles(items, edition) }
    val executeDraftContentApiQuery: Future[Result] =
      config.contentApiQuery.map(executeContentApiQueryViaCache(_, edition)).getOrElse(Future.successful(Result.empty))
    val collectionMetaData: Future[CollectionMeta] = collectionJson.map(getCollectionMeta)

    for {
      curatedRequest <- curatedItems
      executeRequest <- executeDraftContentApiQuery
      collectionMeta <- collectionMetaData
    } yield Collection(
      curated = curatedRequest,
      editorsPicks = executeRequest.editorsPicks.map(makeContent),
      mostViewed = executeRequest.mostViewed.map(makeContent),
      results = executeRequest.contentApiResults.map(makeContent),
      displayName = collectionMeta.displayName,
      href = collectionMeta.href,
      lastUpdated = collectionMeta.lastUpdated,
      updatedBy = collectionMeta.updatedBy,
      updatedEmail = collectionMeta.updatedEmail
    )
  }

  private def getCollectionMeta(collectionJson: JsValue): CollectionMeta =
    CollectionMeta(
        (collectionJson \ "lastUpdated").asOpt[String],
        (collectionJson \ "updatedBy").asOpt[String],
        (collectionJson \ "updatedEmail").asOpt[String],
        (collectionJson \ "displayName").asOpt[String],
        (collectionJson \ "href").asOpt[String]
    )

  private def responseToJson(response: Response): JsValue = {
    response.status match {
      case 200 =>
        Try(parse(response.body)).getOrElse(JsNull)
      case 403 =>
        S3AuthorizationError.increment()
        val errorString: String = s"Request failed to authenticate with S3: ${response.getAHCResponse.getUri}"
        log.warn(errorString)
        throw new Exception(errorString)
      case httpResponseCode: Int if httpResponseCode >= 500 =>
        throw new Exception("S3 returned a 5xx")
      case _ =>
        log.warn(s"Could not load running order: ${response.status} ${response.statusText} ${response.getAHCResponse.getUri}")
        // NOTE: better way of handling fallback
        JsNull
    }
  }

  def getContentApiItems(ids: Set[String], edition: Edition): Future[Map[String, ApiContent]] = {
    val response = client.search(edition: Edition).ids(ids.mkString(",")).showFields(showFieldsWithBodyQuery)
      .response
      .map(response => (response.results map { result =>
      result.id -> result
    }).toMap)

    response onFailure {
      case jsonParseError: net.liftweb.json.JsonParser.ParseException => {
        ContentApiMetrics.ContentApiJsonParseExceptionMetric.increment()
      }
      case mappingException: net.liftweb.json.MappingException => {
        ContentApiMetrics.ContentApiJsonMappingExceptionMetric.increment()
      }
    }

    response
  }

  def getArticles(collectionItems: Seq[CollectionItem], edition: Edition): Future[List[Content]] = {
    val withSupportingLinks = collectionItems zip collectionItems.map(retrieveSupportingLinks)

    for {
      contentApiItems <- getContentApiItems((withSupportingLinks flatMap { case (collectionItem, supportingLinks) =>
        collectionItem.id :: supportingLinks.map(_.id)
      }).toSet, edition)
    } yield withSupportingLinks.toList map { case (collectionItem, supportingLinks) =>
      val supporting = supportingLinks map { link =>
        Content(
          contentApiItems(link.id),
          Nil,
          link.metaData
        )
      }

      if (collectionItem.isSnap) {
        new Snap(
          collectionItem.id,
          supporting,
          collectionItem.webPublicationDate.getOrElse(DateTime.now),
          collectionItem.metaData.getOrElse(Map.empty)
        )
      } else {
        validateContent(Content(
          contentApiItems(collectionItem.id),
          supporting,
          collectionItem.metaData
        ))
      }
    }
  }

  private def retrieveSupportingLinks(collectionItem: CollectionItem): List[CollectionItem] =
    collectionItem.metaData.map(_.get("supporting").flatMap(_.asOpt[List[JsValue]]).getOrElse(Nil)
      .map(json => CollectionItem((json \ "id").as[String], (json \ "meta").asOpt[Map[String, JsValue]], (json \ "frontPublicationDate").asOpt[DateTime]))
    ).getOrElse(Nil)

  def executeContentApiQueryViaCache(queryString: String, edition: Edition): Future[Result] = {
    lazy val contentApiQuery = executeContentApiQuery(queryString, edition)
    if (FaciaToolCachedContentApiSwitch.isSwitchedOn) {
      MemcachedFallback.withMemcachedFallBack(
        sha256Hex(queryString),
        cacheDuration
      ) {
        contentApiQuery
      }
    } else {
      contentApiQuery
    }

  }

  def executeContentApiQuery(queryString: String, edition: Edition): Future[Result] = {
      val queryParams: Map[String, String] = QueryParams.get(queryString).mapValues {
        _.mkString("")
      }
      val queryParamsWithEdition = queryParams + ("edition" -> queryParams.getOrElse("edition", Edition.defaultEdition.id))

      val backFillResponse: Future[Result] = (queryString match {
        case Path(Seg("search" :: Nil)) =>
          val search = client.search(edition)
            .showElements("all")
            .pageSize(20)
          val newSearch = queryParamsWithEdition.foldLeft(search) {
            case (query, (key, value)) => query.stringParam(key, value)
          }.showFields(showFieldsQuery)
          newSearch.response map { searchResponse =>
            Result(
              curated = Nil,
              editorsPicks = Nil,
              mostViewed = Nil,
              contentApiResults = searchResponse.results
            )
          }
        case Path(id) =>
          val search = client.item(id, edition)
            .showElements("all")
            .showEditorsPicks(true)
            .pageSize(20)
          val newSearch = queryParamsWithEdition.foldLeft(search) {
            case (query, (key, value)) => query.stringParam(key, value)
          }.showFields(showFieldsQuery)
          newSearch.response map { itemResponse =>
            Result(
              curated = Nil,
              editorsPicks = itemResponse.editorsPicks,
              mostViewed = itemResponse.mostViewed,
              contentApiResults = itemResponse.results
            )
          }
      }).recover(executeContentApiQueryRecovery)

    backFillResponse onFailure {
      case t: Throwable => log.warn("Content API Query failed: %s: %s".format(queryString, t.toString))
    }

    backFillResponse
  }

  def makeContent(content: ApiContent): Content = validateContent(Content(content))

  def validateContent(content: Content): Content = {
    Try {
      //These will throw if they don't exist because of unsafe Map.apply
      content.headline.isEmpty
      content.shortUrl.isEmpty
      content
    }.getOrElse {
      FaciaToolMetrics.InvalidContentExceptionMetric.increment()
      throw new InvalidContent(content.id)
    }
  }

  val executeContentApiQueryRecovery: PartialFunction[Throwable, Result] = {
    case apiError: com.gu.openplatform.contentapi.ApiError if apiError.httpStatus == 404 => {
      log.warn(s"Content API Error: 404 for ${apiError.httpMessage}")
      Result.empty
    }
    case apiError: com.gu.openplatform.contentapi.ApiError if apiError.httpStatus == 410 => {
      log.warn(s"Content API Error: 410 for ${apiError.httpMessage}")
      Result.empty
    }
    case e: Exception => {
      log.warn(s"ExecuteContentApiQueryRecovery: $e")
      throw e
    }
  }
}

object LiveCollections extends ParseCollection {
  def retrieveItemsFromCollectionJson(collectionJson: JsValue): Seq[CollectionItem] =
    (collectionJson \ "live").asOpt[Seq[JsObject]].getOrElse(Nil).map { trail =>
      CollectionItem(
        (trail \ "id").as[String],
        (trail \ "meta").asOpt[Map[String, JsValue]],
        (trail \ "frontPublicationDate").asOpt[DateTime])
    }

  override val client: ContentApiClient = LiveContentApi
}
