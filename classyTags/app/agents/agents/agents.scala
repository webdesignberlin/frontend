package feed

import conf.{ClassyTag, LiveContentApi}
import common._
import model.Content
import play.api.{ Application => PlayApp, GlobalSettings }
import tags._
import scala.concurrent.Future
import play.api.libs.ws._
import java.net.URLEncoder.encode
import play.api.libs.json._

object LatestContentAgent extends Logging with ExecutionContexts {

  // maps editions to latest content
  private val agent = AkkaAgent[List[ClassyTag]](Nil)

  def latestContent: Seq[ClassyTag] = agent()

  def stop() {
    agent.close()
  }

  def update() {
    log.info("Updating latest content.")

    val newFetchedContent = LiveContentApi.search(Edition.defaultEdition).response.map(_.results.take(1).map(Content(_)))

    val searchedContent: Future[List[ClassyTag]] = newFetchedContent.flatMap( items => {
      val newTags: List[Future[ClassyTag]] = items.map(content => {
        val keywords: Seq[String] = content.tags.filter(_.tagType == "keyword").map(_.webTitle).take(1)

        val fbResponses: Future[Seq[Thing]] = Future.sequence(keywords.map(keyword => socialGraphSearch(keyword))).map(_.flatten)
        fbResponses.map(thingsList => ClassyTag(content, thingsList))
      })

      Future.sequence(newTags)
    })

    searchedContent.map( (superTags: List[ClassyTag]) => {
      agent.update(superTags)
    })
  }

  def getCachedObjects(): Map[String, JsValue] = {
    Map.empty
  }

  def addCache(keyword: String, value: JsValue) {
    val newMap: Map[String, JsValue] = getCachedObjects() + (keyword -> value)
    JsObject(newMap.toSeq)
  }

  def socialGraphSearch(keyword: String) : Future[Option[Thing]] = {
    WS.url("https://graph.facebook.com/search")
      .withQueryString(
        ("q", encode(keyword, "UTF-8")),
        ("type", "page"),
        ("access_token", "CAACEdEose0cBABwUxTmufdye26gn04iZAZCEN0jZBmeQIagXGPQKjaYdxeNKDKYWGOXE7wnchVAITCuVROZCZBN4xExZAn2xfEnpZBGPCD7Ve4EHl9M3dUSswWzEMll3OeOhGWLOaVGei6XMrZCLnSljZBTyneITi7HZBEcpeiyymbIZApZBfZAl7o6dyoEj0X4xMNx8ZD")
      )
      .get().map(resp => {
        val returnedData: JsValue = Json.parse(resp.body)
        val results: Seq[JsValue] = (returnedData \ "data" ).as[Seq[JsValue]]
        results.headOption.map ( result => {
          val name: String = (result \ "name").as[String]
          val category: String = (result \ "category").as[String]

          log.info(s"Adding content ${name} - ${category}")

          new tags.Event(name, category)
        })
      })
  }
}

trait TagAgentsLifecycle extends GlobalSettings {
  override def onStart(app: PlayApp) {
    super.onStart(app)

    //Jobs.deschedule("AgentsRefreshJob")

    // fire every min
    //Jobs.schedule("AgentsRefreshJob", "0 * * * * ?") {
      //LatestContentAgent.update()
    //}


    AkkaAsync {
      //LatestContentAgent.update()
    }
  }

  override def onStop(app: PlayApp) {
    //Jobs.deschedule("AgentsRefreshJob")

    LatestContentAgent.stop()

    super.onStop(app)
  }
}
