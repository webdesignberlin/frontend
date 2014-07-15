package controllers

import feed.LatestContentAgent
import play.api.mvc.{ Content => _, _ }
import common.{JsonComponent, Logging, ExecutionContexts}
import play.api.libs.json.{Json, JsArray}

object ClassyTagsController extends Controller with Logging with ExecutionContexts {

  implicit val itemWrites = Json.writes[LittleItem]
  case class LittleItem(url: String, tags: Seq[String], supertags: Seq[String])

  def dashboard() = Action { implicit request =>
    val latestContent = LatestContentAgent.latestContent
    Ok(views.html.dashboard(latestContent))
  }

  def content() = Action { implicit request =>
    JsonComponent(
      "items" -> JsArray(feed.LatestContentAgent.latestContent.map( classyTag => {
        val littleItem = LittleItem(classyTag.item.url, classyTag.item.tags.filter(_.tagType == "keyword").map(_.webTitle), classyTag.superTags.map(_.name))
        Json.toJson(littleItem)
      }))
    )
  }
}