import conf.Filters
import dev.DevParametersLifecycle
import play.api.mvc.WithFilters

object Global
  extends WithFilters(Filters.common: _*)
  with DevParametersLifecycle
  with feed.TagAgentsLifecycle {

}
