package com.mbta.tid.mbta_app.android.widget

import android.content.Context
import com.mbta.tid.mbta_app.android.BuildConfig
import org.json.JSONObject

/**
 * Debug log (NDJSON) for widget troubleshooting. Only active in debug builds. Pull via: adb
 * exec-out run-as com.mbta.tid.mbta_app cat files/widget_debug.log
 */
internal fun debugSessionLog(
    context: Context,
    location: String,
    message: String,
    data: Map<String, Any?>,
) {
    if (!BuildConfig.DEBUG) return
    try {
        val dir = context.applicationContext.filesDir ?: return
        val f = java.io.File(dir, "widget_debug.log")
        val dataJson = JSONObject()
        data.forEach { (k, v) -> dataJson.put(k, v) }
        val line =
            JSONObject()
                .put("location", location)
                .put("message", message)
                .put("data", dataJson)
                .put("timestamp", System.currentTimeMillis())
                .toString()
        f.appendText(line + "\n")
    } catch (_: Exception) {}
}

internal fun widgetLog(context: Context, tag: String, data: Map<String, Any?>) {
    if (!BuildConfig.DEBUG) return
    try {
        val dir = context.applicationContext.filesDir ?: return
        val f = java.io.File(dir, "widget_debug.log")
        val d =
            data.entries.joinToString(",") { (k, v) ->
                val vStr =
                    when (v) {
                        is String -> "\"${v.replace("\"", "\\\"")}\""
                        else -> v.toString()
                    }
                "\"$k\":$vStr"
            }
        f.appendText("""{"t":"$tag","d":{$d},"ts":${System.currentTimeMillis()}}""" + "\n")
    } catch (_: Exception) {}
}
