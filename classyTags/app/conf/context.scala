package conf

import com.gu.management._
import com.gu.management.play.{ Management => GuManagement }
import model.Content
import tags.{Event, Place, Person, Thing}

case class ClassyTag(item: Content, superTags: Seq[Thing]) {
  def getPeople: Seq[Person] = superTags.collect { case p: Person => p}
  def getPlaces: Seq[Place] = superTags.collect { case p: Place => p}
  def getEvents: Seq[Event] = superTags.collect { case e: Event => e}
}

object Management extends GuManagement {
  val applicationName = "frontend-classy-tags"

  lazy val pages = List(
    new ManifestPage,
    StatusPage(applicationName, Nil)
  )
}