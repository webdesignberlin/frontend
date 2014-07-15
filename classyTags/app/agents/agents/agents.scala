package feed

import conf.LiveContentApi
import common._
import model.Content
import play.api.{ Application => PlayApp, GlobalSettings }
import tags._
import scala.concurrent.Future
import play.api.libs.ws._
import java.net.URLEncoder.encode
import play.api.libs.json._

case class ClassyTag(item: Content, superTags: Seq[Thing])

object LatestContentAgent extends Logging with ExecutionContexts {

  // maps editions to latest content
  private val agent = AkkaAgent[List[ClassyTag]](Nil)

  def latestContent: Seq[ClassyTag] = agent()

  def stop() {
    agent.close()
  }

  def update() {
    log.info("Updating latest content.")

    val newFetchedContent = LiveContentApi.search(Edition.defaultEdition).response.map(_.results.map(Content(_)))

    val searchedContent: Future[List[ClassyTag]] = newFetchedContent.flatMap( items => {
      val newTags: List[Future[ClassyTag]] = items.map(content => {
        val keywords: Seq[String] = content.tags.filter(_.tagType == "keyword").map(_.webTitle)

        val fbResponses: Future[Seq[Thing]] = Future.sequence(keywords.map(keyword => socialGraphSearch(keyword))).map(_.flatten)
        fbResponses.map(thingsList => ClassyTag(content, Nil))
      })

      Future.sequence(newTags)
    })

    searchedContent.map( (superTags: List[ClassyTag]) => {
      agent.update(superTags)
    })
  }

  def socialGraphSearch(keyword: String) : Future[Option[Thing]] = {
    WS.url("https://graph.facebook.com/search")
      .withQueryString(
        ("q", encode(keyword, "UTF-8")),
        ("type", "page"),
        ("access_token", "")
      )
      .get().map(resp => {
        val returnedData: JsValue = Json.parse(resp.body)
        val results: Seq[JsValue] = (returnedData \ "data" ).as[Seq[JsValue]]
        results.headOption.map ( result => {
          val name: String = (result \ "name").as[String]
          new tags.Event(name)
        })
      })
  }
}

trait TagAgentsLifecycle extends GlobalSettings {
  override def onStart(app: PlayApp) {
    super.onStart(app)

    Jobs.deschedule("AgentsRefreshJob")

    // fire every min
    Jobs.schedule("OnwardJourneyAgentsRefreshJob", "0 * * * * ?") {
      LatestContentAgent.update()
    }


    AkkaAsync {
      LatestContentAgent.update()
    }
  }

  override def onStop(app: PlayApp) {
    Jobs.deschedule("AgentsRefreshJob")

    LatestContentAgent.stop()

    super.onStop(app)
  }
}
