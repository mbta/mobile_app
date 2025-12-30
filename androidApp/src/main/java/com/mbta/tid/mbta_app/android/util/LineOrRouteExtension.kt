package com.mbta.tid.mbta_app.android.util

import android.content.res.Resources
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteType

fun LineOrRoute.labelWithModeIfBus(resources: Resources): String =
    if (this.type == RouteType.BUS) resources.getString(R.string.bus_label, this.name)
    else this.name
