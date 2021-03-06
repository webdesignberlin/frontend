import common.{CloudWatchApplicationMetrics, ContentApiMetrics}
import conf.{Configuration, Filters}
import dev.DevParametersLifecycle
import dfp.DfpAgentLifecycle
import ophan.SurgingContentAgentLifecycle
import play.api.mvc.WithFilters

object Global
  extends WithFilters(Filters.common: _*)
  with DevParametersLifecycle
  with DfpAgentLifecycle
  with CloudWatchApplicationMetrics
  with SurgingContentAgentLifecycle {
  override lazy val applicationName = "frontend-article"

  override def applicationMetrics: Map[String, Double] = super.applicationMetrics ++ Map(
    ("elastic-content-api-calls", ContentApiMetrics.ElasticHttpTimingMetric.getAndReset.toDouble),
    ("elastic-content-api-timeouts", ContentApiMetrics.ElasticHttpTimeoutCountMetric.getAndReset.toDouble)
  )
}
