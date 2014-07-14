package feed

import conf.LiveContentApi
import common._
import model.Content
import play.api.{ Application => PlayApp, GlobalSettings }
import tags._

case class classyTag(item: Content, superTags: Seq[Thing])

object LatestContentAgent extends Logging with ExecutionContexts {

  // maps editions to latest content
  private val agent = AkkaAgent[List[Content]](Nil)

  def latestContent: Seq[Content] = agent()

  def stop() {
    agent.close()
  }

  def update() {
    log.info("Updating latest content.")

    val newFetchedContent = LiveContentApi.search(Edition.defaultEdition).response.map(_.results.map(Content(_)))

    newFetchedContent.map( items =>
        agent.update(items)
    )
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
