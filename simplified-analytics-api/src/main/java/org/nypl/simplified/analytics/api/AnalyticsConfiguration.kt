package org.nypl.simplified.analytics.api

import android.content.Context
import org.nypl.simplified.http.core.HTTPType

/**
 * General configuration for analytics systems.
 */

data class AnalyticsConfiguration(

  /**
   * An Android context, used by systems if necessary.
   */

  val context: Context,

  /**
   * The HTTP interface used for analytics requests, if necessary.
   */

  val http: HTTPType
)
