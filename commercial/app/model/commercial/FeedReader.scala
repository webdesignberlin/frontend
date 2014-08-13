package model.commercial

import java.lang.System.currentTimeMillis

import com.ning.http.client.{Response => AHCResponse}
import com.ning.http.util.AsyncHttpProviderUtils.DEFAULT_CHARSET
import common.{ExecutionContexts, Logging}
import conf.Switch
import model.diagnostics.CloudWatch
import play.api.libs.json._
import play.api.libs.ws.WS

import scala.concurrent.Future
import scala.concurrent.duration._

object FeedReader extends ExecutionContexts with Logging {

  import play.api.Play.current

  implicit def duration2Millis(d: Duration): Int = d.toMillis.toInt

  private def recordLoad(feed: Feed, duration: Long) {
    val key = s"${feed.name.toLowerCase.replaceAll("\\s+", "-")}-feed-load-time"
    CloudWatch.put("Commercial", Map(s"$key" -> duration.toDouble))
  }

  private def readBody(feed: Feed): Future[Option[String]] = {
    val start = currentTimeMillis
    val futureResponse = WS.url(feed.url) withRequestTimeout feed.loadTimeout get()

    // TODO return value on failure
    futureResponse onFailure {
      case e: Exception =>
        log.error(s"Loading ${feed.name} from ${feed.url} failed: ${e.getMessage}")
        recordLoad(feed, -1)
    }

    futureResponse map { response =>
      if (response.status == 200) {
        recordLoad(feed, currentTimeMillis - start)
        val body = {
          // look at documentation of response.body to see why this is necessary
          if (feed.characterEncoding == DEFAULT_CHARSET) {
            response.body
          } else {
            response.underlying[AHCResponse].getResponseBody(feed.characterEncoding)
          }
          }
        Some(body)
      } else {
        log.error(s"Loading ${feed.name} ads from ${feed.url} failed: status ${response.status} [${response.statusText}]")
        recordLoad(feed, -1)
        None
        }
      }
  }

  private def read[M](feed: Feed)(parse: String => Seq[M]): Future[Seq[M]] = {
    if (feed.switch.isSwitchedOn) {
      readBody(feed) map {
        _ map { body =>
          val model = parse(body)
          log.info(s"Loaded ${model.size} ${feed.name} from ${feed.url}")
          model
        } getOrElse Nil
      }
    } else {
      log.warn(s"Feed for ${feed.name} switched off")
      Future.successful(Nil)
    }
  }

  def readFromJson[M](feed: Feed)(implicit reads: Reads[M]): Future[Seq[M]] = {
    read(feed) { body =>
      Json.parse(body).validate[Seq[M]] match {
        case s: JsSuccess[Seq[M]] => s.value
        case e: JsError =>
          log.error(s"Parsing ${feed.name} from ${feed.url} failed: ${e.errors.map { case (path, errs) => s"$path: ${errs.head.message}"}.mkString}")
          Nil
      }
    }
  }

  // TODO
  def readFromXml[M](feed: Feed): Future[Option[M]] = Future.successful(None)
}


case class Feed(switch: Switch, name: String, url: String, characterEncoding: String = DEFAULT_CHARSET, loadTimeout: Duration = 2.seconds)
