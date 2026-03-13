package com.mbta.tid.mbta_app.android.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay

class WidgetUpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val requestedIds = inputData.getIntArray(KEY_APP_WIDGET_IDS)
        debugSessionLog(
            applicationContext,
            "WidgetUpdateWorker.doWork",
            "worker_start",
            mapOf("requestedIds" to (requestedIds?.toList()?.toString() ?: "null")),
        )
        val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
        val componentName = ComponentName(applicationContext, MBTATripWidgetReceiver::class.java)
        val appWidgetIds =
            requestedIds?.toList() ?: appWidgetManager.getAppWidgetIds(componentName).toList()

        if (appWidgetIds.isEmpty()) return Result.success()

        val updateIntent =
            Intent(applicationContext, MBTATripWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds.toIntArray())
            }
        applicationContext.sendBroadcast(updateIntent)

        val glanceAppWidgetManager = GlanceAppWidgetManager(applicationContext)
        val widget = MBTATripWidget()

        for (appWidgetId in appWidgetIds) {
            updateWithRetry(glanceAppWidgetManager, widget, appWidgetId)
        }

        return Result.success()
    }

    private suspend fun updateWithRetry(
        glanceManager: GlanceAppWidgetManager,
        widget: MBTATripWidget,
        appWidgetId: Int,
    ) {
        var lastError: String? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val glanceId = glanceManager.getGlanceIdBy(appWidgetId)
                widget.update(applicationContext, glanceId)
                debugSessionLog(
                    applicationContext,
                    "WidgetUpdateWorker.updateWithRetry",
                    "update_success",
                    mapOf("appWidgetId" to appWidgetId, "attempt" to attempt),
                )
                return
            } catch (e: IllegalArgumentException) {
                lastError = e.message ?: e.toString()
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            }
        }
        debugSessionLog(
            applicationContext,
            "WidgetUpdateWorker.updateWithRetry",
            "update_failed_all_retries",
            mapOf("appWidgetId" to appWidgetId, "lastError" to (lastError ?: "unknown")),
        )
    }

    companion object {
        const val WORK_NAME = "WidgetUpdate"
        const val KEY_APP_WIDGET_IDS = "appWidgetIds"
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 300L
    }
}
