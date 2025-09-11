@file:JsExport

package com.mbta.tid.mbta_app.wrapper

import kotlin.js.Date

public data class State(
    val directions: List<Direction>?,
    val upcomingTripTiles: List<Tile>?,
    val tripId: String?,
    val tripHeadsign: String?,
    val tripVehicle: Vehicle?,
    val stopList: StopList?,
) {
    public data class Direction(val name: String?, val destination: String?, val id: Int)

    public data class Tile(
        val tileId: String,
        val tripId: String,
        val headsign: String?,
        val format: UpcomingFormat?,
    )

    public sealed class UpcomingFormat {
        public data class Disruption(val effect: String) : UpcomingFormat()

        public data class Overridden(val text: String) : UpcomingFormat()

        public data object Hidden : UpcomingFormat()

        public data object Boarding : UpcomingFormat()

        public data object Arriving : UpcomingFormat()

        public data object Approaching : UpcomingFormat()

        public data object Now : UpcomingFormat()

        public data class Time(val predictionTime: Date) : UpcomingFormat()

        public data class TimeWithStatus(val predictionTime: Date, val status: String) :
            UpcomingFormat()

        public data class TimeWithSchedule(val predictionTime: Date, val scheduledTime: Date) :
            UpcomingFormat()

        public data class Minutes(val minutes: Int) : UpcomingFormat()

        public data class ScheduleTime(val scheduledTime: Date) : UpcomingFormat()

        public data class ScheduleTimeWithStatus(val scheduledTime: Date, val status: String) :
            UpcomingFormat()

        public data class ScheduleMinutes(val minutes: Int) : UpcomingFormat()

        public data class Skipped(val scheduledTime: Date?) : UpcomingFormat()

        public data class Cancelled(val scheduledTime: Date) : UpcomingFormat()
    }

    public data class Vehicle(
        val stopId: String?,
        val stopName: String?,
        val currentStatus: Status,
    ) {
        public enum class Status {
            IncomingAt,
            StoppedAt,
            InTransitTo,
        }
    }

    public data class StopList(
        val firstStop: Entry?,
        val collapsedStops: List<Entry>?,
        val targetStop: Entry?,
        val followingStops: List<Entry>?,
    ) {
        public data class Entry(
            val entryId: String,
            val stopId: String,
            val stopName: String,
            val format: UpcomingFormat?,
        )
    }
}
