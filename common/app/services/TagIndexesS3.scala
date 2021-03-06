package services

import conf.Configuration
import model.{TagIndexListings, TagIndexPage}
import play.api.Play
import play.api.Play.current
import play.api.libs.json._

import scala.collection.JavaConversions._

sealed trait TagIndexError

case object TagIndexNotFound extends TagIndexError
case class TagIndexReadError(error: JsError) extends TagIndexError

object TagIndexesS3 extends S3 {
  override lazy val bucket = Configuration.indexes.tagIndexesBucket

  val stage = if (Play.isTest) "TEST" else Configuration.facia.stage

  val ListingKey = "_listing"

  private def indexRoot(indexType: String) = s"$stage/index/$indexType"

  private def indexKey(indexType: String, pageName: String) =
    s"${indexRoot(indexType)}/$pageName.json"

  private def putJson[A: Writes](key: String, a: A) = putPublic(
    key,
    Json.stringify(Json.toJson(a)),
    "application/json"
  )

  def putIndex(indexType: String, tagPage: TagIndexPage) = putJson(
    indexKey(indexType, tagPage.id),
    tagPage
  )

  def putListing(indexType: String, listing: TagIndexListings) = putJson(
    indexKey(indexType, ListingKey),
    listing
  )

  private def getAndRead[A: Reads](key: String): Either[TagIndexError, A] = get(key) match {
    case Some(jsonString) =>
      Json.fromJson[A](Json.parse(jsonString)) match {
        case JsSuccess(tagPage, _) => Right(tagPage)
        case error @ JsError(_) => Left(TagIndexReadError(error))
      }

    case None =>
      Left(TagIndexNotFound)
  }

  def getIndex(indexType: String, pageName: String): Either[TagIndexError, TagIndexPage] =
    getAndRead[TagIndexPage](indexKey(indexType, pageName))

  def getListing(indexType: String): Either[TagIndexError, TagIndexListings] =
    getAndRead[TagIndexListings](indexKey(indexType, ListingKey))

  def getListingOrDie(indexType: String): TagIndexListings =
    getListing(indexType) match {
      case Right(listings) => listings
      case Left(TagIndexNotFound) => throw new RuntimeException(s"Could not find index $indexType")
      case Left(TagIndexReadError(_)) => throw new RuntimeException(s"Could not deserialize index $indexType")
    }
}
