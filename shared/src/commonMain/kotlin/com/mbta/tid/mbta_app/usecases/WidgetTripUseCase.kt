package com.mbta.tid.mbta_app.usecases

import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.WidgetTripData
import com.mbta.tid.mbta_app.model.WidgetTripOutput
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISchedulesRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant

/**
 * Use case for finding the next trip from one stop to another for the widget.
 *
 * Uses schedule data (no real-time predictions) since widgets cannot maintain WebSocket
 * connections.
 */
public class WidgetTripUseCase(
    private val globalRepository: IGlobalRepository,
    private val schedulesRepository: ISchedulesRepository,
) {

    public suspend fun getNextTrip(
        fromStopId: String,
        toStopId: String,
        now: EasternTimeInstant = EasternTimeInstant.now(),
    ): ApiResult<WidgetTripOutput> {
        val globalResult = globalRepository.getGlobalData()
        val globalData =
            when (globalResult) {
                is ApiResult.Ok -> globalResult.data
                is ApiResult.Error ->
                    return ApiResult.Error(code = globalResult.code, message = globalResult.message)
            }

        val fromStop = globalData.getStop(fromStopId) ?: return ApiResult.Ok(WidgetTripOutput(null))
        val toStop = globalData.getStop(toStopId) ?: return ApiResult.Ok(WidgetTripOutput(null))

        val fromStopIds =
            listOf(fromStopId) + fromStop.childStopIds.filter { globalData.stops.containsKey(it) }
        val toStopIds =
            listOf(toStopId) + toStop.childStopIds.filter { globalData.stops.containsKey(it) }
        val requestStopIds = (fromStopIds + toStopIds).distinct()

        val scheduleResult = schedulesRepository.getSchedule(requestStopIds, now)
        val scheduleResponse =
            when (scheduleResult) {
                is ApiResult.Ok -> scheduleResult.data
                is ApiResult.Error ->
                    return ApiResult.Error(
                        code = scheduleResult.code,
                        message = scheduleResult.message,
                    )
            }

        val tripData = findNextTrip(scheduleResponse, globalData, fromStopId, toStopId, now)
        return ApiResult.Ok(WidgetTripOutput(tripData))
    }

    private fun findNextTrip(
        response: ScheduleResponse,
        globalData: GlobalResponse,
        fromStopId: String,
        toStopId: String,
        now: EasternTimeInstant,
    ): WidgetTripData? {
        val stops = globalData.stops
        val fromSchedules =
            response.schedules.filter { Stop.equalOrFamily(it.stopId, fromStopId, stops) }
        val toSchedules =
            response.schedules.filter { Stop.equalOrFamily(it.stopId, toStopId, stops) }

        val tripPairs =
            fromSchedules.flatMap { fromSchedule ->
                toSchedules
                    .filter {
                        it.tripId == fromSchedule.tripId &&
                            it.stopSequence > fromSchedule.stopSequence
                    }
                    .map { toSchedule -> fromSchedule to toSchedule }
            }

        val nextTrip =
            tripPairs
                .filter { (from, _) ->
                    val depTime = from.departureTime ?: from.arrivalTime ?: return@filter false
                    depTime >= now
                }
                .minByOrNull { (from, _) ->
                    val depTime = from.departureTime ?: from.arrivalTime!!
                    depTime.instant
                } ?: return null

        val (fromSchedule, toSchedule) = nextTrip
        val trip = response.trips[fromSchedule.tripId] ?: return null
        val route = globalData.getRoute(trip.routeId) ?: return null

        val fromStop = globalData.getStop(fromStopId)!!.resolveParent(globalData)
        val toStop = globalData.getStop(toStopId)!!.resolveParent(globalData)
        val fromScheduleStop = stops[fromSchedule.stopId] ?: fromStop
        val toScheduleStop = stops[toSchedule.stopId] ?: toStop

        val departureTime = fromSchedule.departureTime ?: fromSchedule.arrivalTime ?: return null
        // arrivalTime is the scheduled time at the user's destination stop (toStopId), NOT the
        // end-of-line. toSchedule comes from toSchedules filtered by equalOrFamily(toStopId).
        val arrivalTime = toSchedule.arrivalTime ?: toSchedule.departureTime ?: return null

        val minutesUntil = (departureTime - now).inWholeMinutes.toInt().coerceAtLeast(0)

        // Show track whenever platformCode exists for Commuter Rail (widget shows both board and
        // exit).
        // Previously used shouldShowTrackNumber (CR core stations only); now show for all CR stops.
        val fromPlatform =
            if (fromScheduleStop.vehicleType == RouteType.COMMUTER_RAIL)
                fromScheduleStop.platformCode
            else null
        val toPlatform =
            if (toScheduleStop.vehicleType == RouteType.COMMUTER_RAIL) toScheduleStop.platformCode
            else null

        return WidgetTripData(
            fromStop = fromStop,
            toStop = toStop,
            route = route,
            tripId = trip.id,
            departureTime = departureTime,
            arrivalTime = arrivalTime,
            minutesUntil = minutesUntil,
            fromPlatform = fromPlatform,
            toPlatform = toPlatform,
            headsign = fromSchedule.stopHeadsign ?: trip.headsign,
        )
    }
}
