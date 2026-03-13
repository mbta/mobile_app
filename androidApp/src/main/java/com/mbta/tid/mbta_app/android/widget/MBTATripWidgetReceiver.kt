package com.mbta.tid.mbta_app.android.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MBTATripWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = MBTATripWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val widgetPreferences = WidgetPreferences(context.applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            appWidgetIds.forEach { widgetPreferences.removeConfig(it) }
        }
    }
}
