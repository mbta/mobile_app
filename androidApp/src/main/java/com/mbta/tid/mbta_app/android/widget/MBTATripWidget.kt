package com.mbta.tid.mbta_app.android.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mbta.tid.mbta_app.android.MainActivity
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.formattedTime
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.model.WidgetTripConfig
import com.mbta.tid.mbta_app.model.WidgetTripData
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.usecases.WidgetTripUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MBTATripWidget : GlanceAppWidget(), KoinComponent {

    private val widgetTripUseCase: WidgetTripUseCase by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        try {
            provideGlanceInternal(context, id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            android.util.Log.e("MBTATripWidget", "provideGlance failed", e)
            provideContent { WidgetContent.ErrorState(context = context) }
        }
    }

    private suspend fun provideGlanceInternal(context: Context, id: GlanceId) {
        val widgetPreferences = WidgetPreferences(context.applicationContext)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        debugSessionLog(
            context,
            "MBTATripWidget.provideGlanceInternal",
            "provideGlance_start",
            mapOf(
                "appWidgetId" to appWidgetId,
                "invalid" to (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID),
            ),
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            provideContent { WidgetContent.ErrorState(context = context) }
            return
        }

        var config = withContext(Dispatchers.IO) { widgetPreferences.getConfigOnce(appWidgetId) }
        var retryCount = 0
        if (config == null) {
            for (_i in 0 until 8) {
                delay(250)
                retryCount = _i + 1
                config =
                    withContext(Dispatchers.IO) { widgetPreferences.getConfigOnce(appWidgetId) }
                if (config != null) break
            }
        }
        debugSessionLog(
            context,
            "MBTATripWidget.provideGlanceInternal",
            "config_check_result",
            mapOf(
                "appWidgetId" to appWidgetId,
                "configFound" to (config != null),
                "retryCount" to retryCount,
            ),
        )
        if (config == null) {
            widgetLog(context, "show_configure", mapOf("appWidgetId" to appWidgetId))
            withContext(Dispatchers.IO) {
                WidgetPreferences(context.applicationContext).setPendingConfigWidgetId(appWidgetId)
            }
            provideContent {
                WidgetContent.ConfigurePrompt(context = context, appWidgetId = appWidgetId)
            }
            return
        }

        widgetLog(context, "config_ok", mapOf("appWidgetId" to appWidgetId))
        val result =
            withContext(Dispatchers.IO) {
                widgetTripUseCase.getNextTrip(config.fromStopId, config.toStopId)
            }

        when (result) {
            is ApiResult.Error -> {
                widgetLog(
                    context,
                    "trip_error",
                    mapOf(
                        "appWidgetId" to appWidgetId,
                        "code" to result.code,
                        "msg" to (result.message ?: ""),
                    ),
                )
                provideContent { WidgetContent.ErrorState(context = context) }
            }
            is ApiResult.Ok -> {
                val tripData = result.data.trip
                widgetLog(
                    context,
                    "trip_ok",
                    mapOf("appWidgetId" to appWidgetId, "hasTrip" to (tripData != null)),
                )
                provideContent {
                    if (tripData != null) {
                        WidgetContent.TripData(
                            context = context,
                            config = config,
                            tripData = tripData,
                        )
                    } else {
                        WidgetContent.NoTrips(context = context, config = config)
                    }
                }
            }
        }
    }
}

private object WidgetContent {

    @androidx.compose.runtime.Composable
    fun ConfigurePrompt(context: Context, appWidgetId: Int) {
        val primaryColor =
            Color(ContextCompat.getColor(context, R.color.key).toLong() and 0xFFFFFFFFL)
        val deemphasizedColor =
            Color(ContextCompat.getColor(context, R.color.deemphasized).toLong() and 0xFFFFFFFFL)
        val bgColor = Color(ContextCompat.getColor(context, R.color.fill2).toLong() and 0xFFFFFFFFL)
        GlanceTheme {
            Column(
                modifier =
                    GlanceModifier.fillMaxWidth()
                        .background(bgColor)
                        .padding(16.dp)
                        .clickable(
                            onClick =
                                actionStartActivity(
                                    WidgetConfigActivity::class.java,
                                    parameters =
                                        actionParametersOf(
                                            ActionParameters.Key<Int>(
                                                AppWidgetManager.EXTRA_APPWIDGET_ID
                                            ) to appWidgetId
                                        ),
                                )
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = context.getString(R.string.widget_configure),
                    style = TextStyle(color = ColorProvider(primaryColor)),
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = context.getString(R.string.widget_set_from_to),
                    style = TextStyle(color = ColorProvider(deemphasizedColor)),
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    fun ErrorState(context: Context) {
        val deemphasizedColor =
            Color(ContextCompat.getColor(context, R.color.deemphasized).toLong() and 0xFFFFFFFFL)
        val bgColor = Color(ContextCompat.getColor(context, R.color.fill2).toLong() and 0xFFFFFFFFL)
        GlanceTheme {
            Column(
                modifier =
                    GlanceModifier.fillMaxWidth()
                        .background(bgColor)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = context.getString(R.string.widget_unable_to_load),
                    style = TextStyle(color = ColorProvider(deemphasizedColor)),
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    fun NoTrips(context: Context, config: WidgetTripConfig) {
        val fromLabel = config.fromLabel.ifEmpty { context.getString(R.string.widget_from) }
        val toLabel = config.toLabel.ifEmpty { context.getString(R.string.widget_to) }
        val primaryColor =
            Color(ContextCompat.getColor(context, R.color.key).toLong() and 0xFFFFFFFFL)
        val deemphasizedColor =
            Color(ContextCompat.getColor(context, R.color.deemphasized).toLong() and 0xFFFFFFFFL)
        val bgColor = Color(ContextCompat.getColor(context, R.color.fill2).toLong() and 0xFFFFFFFFL)
        GlanceTheme {
            Column(
                modifier =
                    GlanceModifier.fillMaxWidth()
                        .background(bgColor)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$fromLabel → $toLabel",
                    style = TextStyle(color = ColorProvider(primaryColor)),
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = context.getString(R.string.widget_no_trips),
                    style = TextStyle(color = ColorProvider(deemphasizedColor)),
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    fun TripData(context: Context, config: WidgetTripConfig, tripData: WidgetTripData) {
        val fromLabel = config.fromLabel.ifEmpty { tripData.fromStop.name }
        val toLabel = config.toLabel.ifEmpty { tripData.toStop.name }
        val defaultColor =
            Color(ContextCompat.getColor(context, R.color.key).toLong() and 0xFFFFFFFFL)
        val routeColor =
            runCatching { Color.fromHex(tripData.route.color) }.getOrElse { defaultColor }
        val routeTextColor =
            runCatching { Color.fromHex(tripData.route.textColor) }.getOrElse { Color.White }
        val trainLabel =
            tripData.headsign?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.widget_train, tripData.tripId)
        val primaryColor =
            Color(ContextCompat.getColor(context, R.color.key).toLong() and 0xFFFFFFFFL)
        val textColor =
            Color(ContextCompat.getColor(context, R.color.text).toLong() and 0xFFFFFFFFL)
        val deemphasizedColor =
            Color(ContextCompat.getColor(context, R.color.deemphasized).toLong() and 0xFFFFFFFFL)
        val bgColor = Color(ContextCompat.getColor(context, R.color.fill2).toLong() and 0xFFFFFFFFL)

        GlanceTheme {
            Column(
                modifier =
                    GlanceModifier.fillMaxWidth()
                        .background(bgColor)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        GlanceModifier.fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(routeColor)
                            .cornerRadius(20.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tripData.route.label,
                            style = TextStyle(color = ColorProvider(routeTextColor)),
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = trainLabel,
                            style = TextStyle(color = ColorProvider(routeTextColor)),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "$fromLabel → $toLabel",
                        style = TextStyle(color = ColorProvider(primaryColor)),
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(R.string.widget_in_minutes, tripData.minutesUntil),
                        style = TextStyle(color = ColorProvider(primaryColor)),
                    )
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text =
                        "${tripData.departureTime.formattedTime()} → ${tripData.arrivalTime.formattedTime()}",
                    style = TextStyle(color = ColorProvider(deemphasizedColor)),
                )
                if (tripData.fromPlatform != null || tripData.toPlatform != null) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    val platformText = buildString {
                        tripData.fromPlatform?.let {
                            append(context.getString(R.string.widget_track_short, it))
                        }
                        if (tripData.fromPlatform != null && tripData.toPlatform != null)
                            append(" • ")
                        tripData.toPlatform?.let {
                            append(context.getString(R.string.widget_track_short, it))
                        }
                    }
                    if (platformText.isNotEmpty()) {
                        Text(
                            text = platformText,
                            style = TextStyle(color = ColorProvider(deemphasizedColor)),
                        )
                    }
                }
            }
        }
    }
}
