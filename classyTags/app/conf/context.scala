package conf

import com.gu.management._
import com.gu.management.play.{ Management => GuManagement }

object Management extends GuManagement {
  val applicationName = "frontend-classy-tags"

  lazy val pages = List(
    new ManifestPage,
    StatusPage(applicationName, Nil)
  )
}