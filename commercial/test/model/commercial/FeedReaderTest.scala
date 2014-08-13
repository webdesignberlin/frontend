package model.commercial

import conf.{Off, On, Switch}
import model.commercial.soulmates.Member
import org.joda.time.LocalDate
import org.scalatest.{FlatSpec, Matchers}
import scala.concurrent.Await
import scala.concurrent.duration._

class FeedReaderTest extends FlatSpec with Matchers {

  private val onSwitch = new Switch(group = "test", name = "switch", description = "test switch", safeState = On, sellByDate = LocalDate.now().plusDays(1))
  private val offSwitch = onSwitch.copy(safeState = Off)

  private val feedName = "Test Things"
  private val feedUrl = "https://soulmates.theguardian.com/microapp/nextgen/popular/men"

  private val testTimeout = 10.seconds

  "readFromJson" should "read items from a Json feed" in {
    val feed = Feed(onSwitch, feedName, feedUrl)
    val model = Await.result(FeedReader.readFromJson[Member](feed), testTimeout)
    model should not be empty
  }

  "readFromJson" should "read no items when switch is off" in {
    val feed = Feed(offSwitch, feedName, feedUrl)
    val model = Await.result(FeedReader.readFromJson[Member](feed), testTimeout)
    model shouldBe empty
  }

  "readFromJson" should "read no items when load times out" in {
    val feed = Feed(onSwitch, feedName, feedUrl, loadTimeout = 1.millis)
    val model = Await.result(FeedReader.readFromJson[Member](feed), testTimeout)
    model shouldBe empty
  }
}
