package conf

import com.gu.management._
import com.gu.management.play.{ Management => GuManagement }
import com.gu.management.logback.LogbackLevelPage

object Management extends GuManagement {
  val applicationName = "frontend-classy-tags"

  lazy val pages = List(
    new ManifestPage,
    StatusPage(applicationName, Nil)
  )
}