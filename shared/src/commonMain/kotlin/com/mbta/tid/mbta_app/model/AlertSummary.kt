package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.serviceDate
import com.mbta.tid.mbta_app.utils.toBostonTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class AlertSummary(
    val effect: Alert.Effect,
    val location: Location? = null,
    val timeframe: Timeframe? = null
) {
    sealed class Location {
        data class BranchingStops(val startStopName: String, val directionName: String) :
            Location()

        data class SingleStop(val stopName: String) : Location()

        data class SuccessiveStops(val startStopName: String, val endStopName: String) : Location()
    }

    sealed class Timeframe {
        data object EndOfService : Timeframe()

        data object Tomorrow : Timeframe()

        data class LaterDate(val time: Instant) : Timeframe()

        data class ThisWeek(val time: Instant) : Timeframe()

        data class Time(val time: Instant) : Timeframe()
    }

    companion object {
        fun summarizing(alert: Alert, atTime: Instant, global: GlobalResponse): AlertSummary? {
            if (alert.significance < AlertSignificance.Secondary) {
                return null
            }

            return AlertSummary(alert.effect, alertLocation(alert), alertTimeframe(alert, atTime))
        }

        private fun alertTimeframe(alert: Alert, atTime: Instant): Timeframe? {
            val currentPeriod = alert.currentPeriod(atTime) ?: return null
            if (currentPeriod.endingLaterToday) return null

            if (currentPeriod.toEndOfService) {
                return Timeframe.EndOfService
            }

            val endTime = currentPeriod.end ?: return null

            val serviceDate = atTime.toBostonTime().serviceDate
            val endDate = endTime.toBostonTime().serviceDate
            if (serviceDate == endDate) {
                return Timeframe.Time(endTime)
            } else if (serviceDate.plus(DatePeriod(days = 1)) == endDate) {
                return Timeframe.Tomorrow
            } else if (laterThisWeek(serviceDate, endDate)) {
                return Timeframe.ThisWeek(endTime)
            }

            return Timeframe.LaterDate(endTime)
        }

        private fun laterThisWeek(onDate: LocalDate, endDate: LocalDate): Boolean =
            onDate.dayOfWeek.isoDayNumber < endDate.dayOfWeek.isoDayNumber &&
                endDate.minus(onDate).days < 7

        private fun alertLocation(alert: Alert): Location? {
            return null
        }
    }
}
