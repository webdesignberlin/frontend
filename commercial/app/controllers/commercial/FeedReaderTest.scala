package controllers.commercial

import common.ExecutionContexts
import model.commercial.FeedReader
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._

object FeedReaderTest extends Controller with ExecutionContexts {

  private implicit val objReads: Reads[Obj] = (
    (JsPath \ "lat").read[Int] and
      (JsPath \ "long").read[String]
    )(Obj.apply _)

  def doIt() = Action.async { implicit request =>
    val url = "https://www.eventbrite.com/json/organizer_list_events?app_key=ZUS3L55RMIF6WC7OTI&id=684756979"
    val futureResult = FeedReader.readFromJson[Obj]("test", url, loadTimeout = 90.seconds)

    futureResult map { result =>
      Ok(result.headOption.map(_.name).getOrElse("Empty"))
    }
  }
}


case class Obj(id: Int, name: String)
