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

        val (work, rest) = contents.get.splitAt(1)

        contents.update(rest)


        val responses: List[Future[ClassyTag]] = work.map(item => {

          val keywords: Seq[String] = item.tags.filter(_.tagType == "keyword").map(_.webTitle)

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
        ("access_token", "CAACEdEose0cBAOTs7cfJ0AHEql6zFSmCJZCZBj8s8cPMz6SxFy0sNjDAXpSpgSL7a97UBIDCnH6sUf32qP5snUkPUO2UZBvTXWexNlhPQgWMZBr7GThA4uVHDh9N6H9HeH57Wr2JgOzFDyCbSZApEJoRrkk5O90tFY7LNZBR3wZCUfhyXnwN2bIG1dXcR9tsG0ZD")
      )
      .get()

    resp.onFailure { case failure: Throwable => log.error(failure.getMessage) }
    resp.onSuccess { case _ => log.info("success") }

    resp.map(resp => {
        val returnedData: JsValue = Json.parse(resp.body)
        val results: Seq[JsValue] = (returnedData \ "data" ).as[Seq[JsValue]]
        results.headOption.map ( result => {
          val name: String = (result \ "name").as[String]
          val category: String = (result \ "category").as[String]

          log.info(s"Adding content ${name} - ${category}")

          createThing(name, category)
        })
      })
  }

  private val places = List("Country", "City")
  private val people = List("Musician/band", "Politician", "Political party", "Athlete")
  private val events = List("Sports event")

  def createThing(name: String, category: String) : tags.Thing = {

    category match {
      case _ if places.contains(category) => tags.Place(name, category)
      case _ if people.contains(category) => tags.Person(name, category)
      case _ if events.contains(category) => tags.Event(name, category)
      case _ => tags.Unknown(name, category)
    }
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
