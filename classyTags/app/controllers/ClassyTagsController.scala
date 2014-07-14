package controllers

import play.api.mvc.{ Content => _, _ }
import common.{Logging, ExecutionContexts}

object ClassyTagsController extends Controller with Logging with ExecutionContexts {

  def dashboard() = Action {
    Ok("in")
  }

  def content() = Action {
    Ok(feed.LatestContentAgent.latestContent.map(_.shortUrlPath).mkString(","))
  }
}