package model.commercial

import com.ning.http.client.{Response => AHCResponse}
import com.ning.http.util.AsyncHttpProviderUtils.DEFAULT_CHARSET
import common.{ExecutionContexts, Logging}
import model.diagnostics.CloudWatch
import play.api.libs.json._
import play.api.libs.ws.WS

import scala.concurrent.Future
import scala.concurrent.duration._

// TODO switch
// TODO transform string before parsing
object FeedReader extends ExecutionContexts with Logging {

  import play.api.Play.current

  private def recordLoad(feedName: String, duration: Long) {
    val key = s"${feedName.toLowerCase.replaceAll("\\s+", "-")}-feed-load-time"
    CloudWatch.put("Commercial", Map(s"$key" -> duration.toDouble))
  }

  private def readBody(feedName: String, url: String, characterEncoding: String, loadTimeout: Duration): Future[Option[String]] = {
    val start = System.currentTimeMillis
    val futureResponse = WS.url(url) withRequestTimeout loadTimeout.toMillis.toInt get()

    futureResponse onFailure {
      case e: Exception =>
        log.error(s"Loading $feedName from $url failed: ${e.getMessage}")
        recordLoad(feedName, -1)
    }

    futureResponse map { response =>
      if (response.status == 200) {
        recordLoad(feedName, System.currentTimeMillis - start)
        val body = {
          // look at documentation of response.body to see why this is necessary
          if (characterEncoding == DEFAULT_CHARSET) {
            response.body
          } else {
            response.underlying[AHCResponse].getResponseBody(characterEncoding)
          }
        }
        Some(body)
      } else {
        log.error(s"Loading $feedName ads from $url failed: status ${response.status} [${response.statusText}]")
        recordLoad(feedName, -1)
        None
      }
    }
  }

  private def read[M](feedName: String, url: String, characterEncoding: String, loadTimeout: Duration)(parse: String => Seq[M]): Future[Seq[M]] = {
    readBody(feedName, url, characterEncoding, loadTimeout) map {
      _ map { body =>
        val model = parse(body)
        log.info(s"Loaded ${model.size} $feedName from $url")
        model
      } getOrElse Nil
    }
  }

  def readFromJson[M](feedName: String, url: String, characterEncoding: String = DEFAULT_CHARSET, loadTimeout: Duration = 2.seconds)
                     (implicit reads: Reads[M]): Future[Seq[M]] = {
    read(feedName, url, characterEncoding, loadTimeout) { body =>
      Json.parse(body).validate[Seq[M]] match {
        case s: JsSuccess[Seq[M]] => s.value
        case e: JsError =>
          log.error(s"Parsing $url failed: ${e.errors.map { case (path, errs) => s"$path: ${errs.head.message}"}.mkString}")
          Nil
      }
    }
  }

  // TODO
  def readFromXml[M](url: String, characterEncoding: String = DEFAULT_CHARSET, loadTimeout: Duration = 2.seconds): Future[Option[M]] = Future.successful(None)
}
