package com.mbta.tid.mbta_app.android.notification

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.mbta.tid.mbta_app.android.stopDetails.AlertCardSpec
import com.mbta.tid.mbta_app.android.util.FormattedAlert
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IAlertsRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

data class NotificationContent(val title: AnnotatedString, val body: AnnotatedString) {
    companion object {
        suspend fun build(
            context: Context,
            alertId: String,
            subscriptions: List<RouteStopDirection>,
            alertsRepository: IAlertsRepository,
            globalRepository: IGlobalRepository,
        ): NotificationContent? {
            val alerts =
                when (val data = alertsRepository.getSnapshot()) {
                    is ApiResult.Ok -> data.data
                    is ApiResult.Error -> return null
                }
            val alert = alerts.getAlert(alertId) ?: return null
            val globalData =
                when (val globalData = globalRepository.getGlobalData()) {
                    is ApiResult.Ok -> globalData.data
                    is ApiResult.Error -> return null
                }
            val routeId = subscriptions.first().route
            val stopId = subscriptions.first().stop
            val directionId = subscriptions.first().direction
            val alertSummary =
                alert.summary(
                    stopId,
                    directionId,
                    globalData.getPatternsFor(
                        stopId,
                        globalData.getLineOrRoute(routeId) ?: return null,
                    ),
                    EasternTimeInstant.now(),
                    globalData,
                )
            val formattedAlert = FormattedAlert(alert, alertSummary)

            val title = formattedAlert.alertCardHeader(AlertCardSpec.Major, context)
            val body = formattedAlert.alertCardMajorBody(context)

            return NotificationContent(title, body)
        }
    }
}
