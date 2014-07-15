package conf

import com.gu.management._
import com.gu.management.play.{ Management => GuManagement }
import model.Content
import tags.Thing

case class ClassyTag(item: Content, superTags: Seq[Thing])

object Management extends GuManagement {
  val applicationName = "frontend-classy-tags"

  lazy val pages = List(
    new ManifestPage,
    StatusPage(applicationName, Nil)
  )
}