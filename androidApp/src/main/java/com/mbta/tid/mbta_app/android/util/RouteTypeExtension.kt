package com.mbta.tid.mbta_app.android.util

import android.content.Context
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.RouteType

/** Pluralized text description of the route type */
fun RouteType.typeText(context: Context, isOnly: Boolean): String {
    return when (this) {
        RouteType.BUS -> context.getString(if (isOnly) R.string.bus_lowercase else R.string.buses)
        RouteType.FERRY ->
            context.getString(if (isOnly) R.string.ferry_lowercase else R.string.ferries)
        else -> context.getString(if (isOnly) R.string.train else R.string.trains)
    }
}
