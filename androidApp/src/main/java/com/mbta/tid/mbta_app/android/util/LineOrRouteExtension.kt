package com.mbta.tid.mbta_app.android.util

import android.content.Context
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteType

fun RouteCardData.LineOrRoute.labelWithModeIfBus(context: Context): String =
    if (this.type == RouteType.BUS) context.getString(R.string.bus_label, this.name) else this.name
