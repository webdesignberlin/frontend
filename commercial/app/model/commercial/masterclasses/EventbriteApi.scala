package model.commercial.masterclasses

import conf.Switches.MasterclassFeedSwitch
import conf.{CommercialConfiguration, Configuration}
import model.commercial.FeedReader
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}
import play.api.libs.json.{JsPath, Reads, JsValue}

import scala.concurrent.Future
import scala.concurrent.duration._
  import play.api.libs.functional.syntax._
import play.api.libs.json._

object EventbriteApi {


  case class EventbriteMasterClass(
                                    id: String,
                                    name: String,
                                    startDate: DateTime,
                                    url: String,
                                    description: String,
                                    status: String,
                                    venue: Venue,
                                    tickets: List[Ticket],
                                    capacity: Int,
                                    guardianUrl: String,
                                    firstParagraph: String = "",
                                    tags: Seq[String],
                                    keywordIds: Seq[String] = Nil



  implicit val eventbriteMasterclassReads: Reads[EventbriteMasterClass] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "title").read[String] and
  (JsPath \ "start_date").read[DateTime] and
   (JsPath \ "url").read[String]
   (JsPath \ "description").read[String]
  (JsPath \ "status").read[String]
   (JsPath \ "venue").read[Venue]

    )(EventbriteMasterClass.apply _ )



  def apply(block: JsValue): Option[EventbriteMasterClass] = {
    val id = (block \ "id").as[Long]
    val title = (block \ "title").as[String]
    val literalDate = (block \ "start_date").as[String]
    val startDate: DateTime = datePattern.parseDateTime(literalDate)
    val url = (block \ "url").as[String]
    val description = (block \ "description").as[String]
    val status = (block \ "status").as[String]
    val capacity = (block \ "capacity").as[Int]

    val tags = {
      for {
        rawTag <- (block \ "tags").as[String].split(",")
        tag = rawTag.toLowerCase.replaceAll("courses?", "").trim()
        if tag.nonEmpty && !ignoredTags.exists(tag.contains)
      } yield tag
    }.toSeq

    val tickets = (block \\ "ticket") map {
      ticket =>
        val price = (ticket \ "display_price").as[String].replace(",", "").toDouble
        new Ticket(price)
    }

    val doc: Document = Jsoup.parse(description)
    val elements: Array[Element] = doc.select(s"a[href^=http://www.theguardian.com/]:contains($guardianUrlLinkText)").toArray map {_.asInstanceOf[Element]}

    val paragraphs: Array[Element] = doc.select("p").toArray map {_.asInstanceOf[Element]}

    elements.headOption.map { element =>
      new EventbriteMasterClass(id.toString,
        title,
        startDate,
        url,
        description,
        status,
        Venue(block \ "venue"),
        tickets.toList,
        capacity,
        element.attr("href"),
        paragraphs.headOption.fold("")(_.text),
        tags
      )
    }
  }




  def loadAds(): Future[Seq[EventbriteMasterClass]] = {

    val optUrl = {
      val env = Configuration.environment
      lazy val devUrl = CommercialConfiguration.getProperty("masterclasses.dev.url")
      if (env.isNonProd) {
        devUrl
      } else {
        CommercialConfiguration.getProperty("masterclasses.api.key") map { apiKey =>
          val organiserId = "684756979"
          s"https://www.eventbrite.com/json/organizer_list_events?app_key=$apiKey&id=$organiserId"
        }
      }
    }

    optUrl map { url =>
      FeedReader.readFromJson[EventbriteMasterClass](
        switch = MasterclassFeedSwitch,
        feedName = "Masterclasses",
        url = url,
        characterEncoding = "utf-8",
        loadTimeout = 60.seconds)
    } getOrElse Future.successful(Nil)
  }

  private def extractEventsFromFeed(jsValue: JsValue): Seq[JsValue] = jsValue \\ "event"

  private def parse(json: JsValue) = {
    val maybes = extractEventsFromFeed(json) map (EventbriteMasterClass(_))
    maybes.flatten
  }
}
