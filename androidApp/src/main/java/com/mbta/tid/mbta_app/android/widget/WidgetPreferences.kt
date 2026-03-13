package com.mbta.tid.mbta_app.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.mbta.tid.mbta_app.model.WidgetTripConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "widget_config"
private const val KEY_PENDING_CONFIG_WIDGET_ID = "pending_config_widget_id"

internal class WidgetPreferences(private val context: Context) {

    private val prefs
        get() = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun configKey(appWidgetId: Int) = "config_$appWidgetId"

    /** Stores appWidgetId so config activity can read it when the intent lacks it. */
    fun setPendingConfigWidgetId(appWidgetId: Int) {
        prefs.edit().putInt(KEY_PENDING_CONFIG_WIDGET_ID, appWidgetId).commit()
    }

    fun getAndClearPendingConfigWidgetId(): Int {
        val id = prefs.getInt(KEY_PENDING_CONFIG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        prefs.edit().remove(KEY_PENDING_CONFIG_WIDGET_ID).commit()
        return id
    }

    private fun getConfigSync(appWidgetId: Int): WidgetTripConfig? {
        val raw = prefs.getString(configKey(appWidgetId), null) ?: return null
        val lines = raw.split("\n")
        if (lines.size < 4) return null
        return WidgetTripConfig(
            fromStopId = lines[0].trim(),
            toStopId = lines[1].trim(),
            fromLabel = lines.getOrNull(2)?.trim() ?: "",
            toLabel = lines.getOrNull(3)?.trim() ?: "",
        )
    }

    suspend fun getConfigOnce(appWidgetId: Int): WidgetTripConfig? =
        withContext(Dispatchers.IO) { getConfigSync(appWidgetId) }

    suspend fun setConfig(appWidgetId: Int, config: WidgetTripConfig) =
        withContext(Dispatchers.IO) {
            val value =
                listOf(config.fromStopId, config.toStopId, config.fromLabel, config.toLabel)
                    .joinToString("\n")
            prefs.edit().putString(configKey(appWidgetId), value).commit()
        }

    suspend fun removeConfig(appWidgetId: Int) =
        withContext(Dispatchers.IO) { prefs.edit().remove(configKey(appWidgetId)).commit() }
}
