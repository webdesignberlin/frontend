package controllers

import play.api.mvc.{ Content => _, _ }
import common.{JsonComponent, Logging, ExecutionContexts}
import play.api.libs.json.{Json, JsArray}

object ClassyTagsController extends Controller with Logging with ExecutionContexts {

  implicit val itemWrites = Json.writes[LittleItem]
  case class LittleItem(url: String, tags: Seq[String])

  def dashboard() = Action { implicit request =>
    Ok("in")
  }

  def content() = Action { implicit request =>
    JsonComponent(
      "items" -> JsArray(feed.LatestContentAgent.latestContent.map( content => {
        val littleItem = LittleItem(content.url, content.tags.filter(_.tagType == "keyword").map(_.webTitle))
        Json.toJson(littleItem)
      }))
    )
  }
}