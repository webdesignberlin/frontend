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


  private val agent = AkkaAgent[List[ClassyTag]](Nil)

  private val contents = AkkaAgent[List[Content]](Nil)


  def latestContent: Seq[ClassyTag] = agent()

  def stop() {
    agent.close()
  }

  def update() {
    log.info("Updating latest content.")

    if (contents.get.size == 0 ) {
      LiveContentApi.search(Edition.defaultEdition).pageSize(100).response.map( resp => {
        contents.update(resp.results.map(Content(_)))
      })
    } else {

      log.info("Fetching keywords.")


      val searchedContent: Future[Seq[ClassyTag]] = {

        val (work, rest) = contents.get.splitAt(3)

        contents.update(rest)


        val responses: List[Future[ClassyTag]] = work.map(item => {

          val keywords: Seq[String] = item.tags.filter(_.tagType == "keyword").map(_.webTitle).take(1)

          val fbResponses: Future[Seq[Thing]] = Future.sequence(keywords.map(keyword => socialGraphSearch(keyword))).map(_.flatten)
          fbResponses.map(thingsList => ClassyTag(item, thingsList))
        })

        Future.sequence(responses)
      }

      searchedContent.map( (superTags: Seq[ClassyTag]) => {
        agent.send( current =>  current ++ superTags)
      })
    }

  }

  def getCachedObjects(): Map[String, JsValue] = {
    Map.empty
  }

  def addCache(keyword: String, value: JsValue) {
    val newMap: Map[String, JsValue] = getCachedObjects() + (keyword -> value)
    JsObject(newMap.toSeq)
  }

  def socialGraphSearch(keyword: String) : Future[Option[Thing]] = {
    log.info("finding supertag for " + keyword)

    val resp = WS.url("https://graph.facebook.com/search")
      .withQueryString(
        ("q", encode(keyword, "UTF-8")),
        ("type", "page"),
        ("access_token", "CAACEdEose0cBALwWfyPVa9vzFaufooZAXWM1PTI6ra6iD7y1Q9ULjmRRdRDuoTXESZBl0y8wR1AjYcalisZC0WdzcZApaJiZBa4IPGtmj0THLZBUKr8TuyokA6lNQpU8uISvl9nkZBWVV9MKaqModP7nQQAKGPq3pNrZAYFPnJa2ou5MgRXEZB9iygy0FZAC42ZAYEZD")
      )
      .get()

    resp.onFailure { case failure: Throwable => log.error(failure.getMessage) }
    resp.onSuccess { case success: Response => log.info(success.body) }

    resp.map(resp => {
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
