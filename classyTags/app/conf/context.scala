package conf

import com.gu.management._
import com.gu.management.play.{ Management => GuManagement }
import model.Content
import tags.{Event, Place, Person, Thing}
import scalaz._,Scalaz._
import scalaz.syntax.monoid._
import scalaz.std.map._

case class ClassyTag(item: Content, superTags: Seq[Thing]) {
  def getPeople: Seq[Person] = superTags.collect { case p: Person => p}
  def getPlaces: Seq[Place] = superTags.collect { case p: Place => p}
  def getEvents: Seq[Event] = superTags.collect { case e: Event => e}

  def getPeopleGroupedByCategory: Map[String, Seq[Person]] = getPeople.groupBy(_.category)
  def getPlacesGroupedByCategory: Map[String, Seq[Place]] = getPlaces.groupBy(_.category)
  def getEventsGroupedByCategory: Map[String, Seq[Event]] = getEvents.groupBy(_.category)
}

object ClassyTag {
  def reduceTheMap(tags: Seq[ClassyTag]) =
    tags.map { _.getEventsGroupedByCategory }.reduce((a,b) => intersectWith(a, b)(_ ++ _))
}

object Management extends GuManagement {
  val applicationName = "frontend-classy-tags"

  lazy val pages = List(
    new ManifestPage,
    StatusPage(applicationName, Nil)
  )
}