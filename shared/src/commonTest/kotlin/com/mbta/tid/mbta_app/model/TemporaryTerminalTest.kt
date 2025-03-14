package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import io.github.dellisd.spatialk.geojson.Position
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** An integration test for all of the logic in [TemporaryTerminalFilter]. */
class TemporaryTerminalTest {
    val now = Instant.parse("2024-08-19T16:44:08-04:00")

    val objects = ObjectCollectionBuilder()
    val red =
        objects.route {
            id = "Red"
            type = RouteType.HEAVY_RAIL
        }

    data class StopWithPlatforms(val northbound: Stop, val southbound: Stop, val station: Stop) {
        val childIds: List<String> = listOf(northbound.id, southbound.id)
    }

    fun stop() =
        PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, StopWithPlatforms>> { _, property ->
            lateinit var northbound: Stop
            lateinit var southbound: Stop
            val station =
                objects.stop {
                    id = "place-${property.name}"
                    northbound = childStop { id = "${property.name}-northbound" }
                    southbound = childStop { id = "${property.name}-southbound" }
                }
            val stopWithPlatforms = StopWithPlatforms(northbound, southbound, station)
            ReadOnlyProperty { _, _ -> stopWithPlatforms }
        }

    val alewife by stop()
    val davis by stop()
    val porter by stop()
    val harvard by stop()
    val central by stop()
    val kendallMit by stop()
    val charlesMgh by stop()
    val parkStreet by stop()
    val downtownCrossing by stop()
    val southStation by stop()
    val broadway by stop()
    val andrew by stop()
    val jfkUmass by stop()
    val savinHill by stop()
    val fieldsCorner by stop()
    val shawmut by stop()
    val ashmont by stop()
    val northQuincy by stop()
    val wollaston by stop()
    val quincyCenter by stop()
    val quincyAdams by stop()
    val braintree by stop()

    val stopsNorthOfShuttle = listOf(alewife, davis, porter, harvard, central, kendallMit)
    val stopsInShuttle =
        listOf(charlesMgh, parkStreet, downtownCrossing, southStation, broadway, andrew)
    val stopsAshmontBranch = listOf(jfkUmass, savinHill, fieldsCorner, shawmut, ashmont)
    val stopsBraintreeBranch =
        listOf(jfkUmass, northQuincy, wollaston, quincyCenter, quincyAdams, braintree)

    fun Iterable<StopWithPlatforms>.southbound() = map(StopWithPlatforms::southbound)

    fun Iterable<StopWithPlatforms>.northbound() = map(StopWithPlatforms::northbound)

    private fun pattern(
        id: String,
        typicality: RoutePattern.Typicality,
        sortOrder: Int,
        headsign: String,
        stops: List<StopWithPlatforms>
    ) =
        objects.routePattern(red) {
            this.id = id
            val directionId = id.substringAfterLast('-')
            this.directionId = directionId.toInt()
            this.typicality = typicality
            this.sortOrder = sortOrder
            representativeTrip {
                this.headsign = headsign
                this.stopIds =
                    if (directionId == "0") {
                            stops.southbound()
                        } else {
                            stops.northbound()
                        }
                        .map { it.id }
            }
        }

    val redAlewifeAshmont =
        pattern(
            "Red-1-0",
            RoutePattern.Typicality.Typical,
            100100001,
            "Ashmont",
            stopsNorthOfShuttle + stopsInShuttle + stopsAshmontBranch
        )
    val redAlewifeBraintree =
        pattern(
            "Red-3-0",
            RoutePattern.Typicality.Typical,
            100100000,
            "Braintree",
            stopsNorthOfShuttle + stopsInShuttle + stopsBraintreeBranch
        )
    val redJfkAshmont =
        pattern(
            "Red-A-0",
            RoutePattern.Typicality.Atypical,
            100100090,
            "Ashmont",
            stopsAshmontBranch
        )
    val redJfkBraintree =
        pattern(
            "Red-B-0",
            RoutePattern.Typicality.Atypical,
            100100140,
            "Braintree",
            stopsBraintreeBranch
        )
    val redAlewifeKendall =
        pattern(
            "Red-S-0",
            RoutePattern.Typicality.Atypical,
            100100150,
            "Kendall/MIT",
            stopsNorthOfShuttle
        )
    val redAshmontAlewife =
        pattern(
            "Red-1-1",
            RoutePattern.Typicality.Typical,
            100101001,
            "Alewife",
            (stopsNorthOfShuttle + stopsInShuttle + stopsAshmontBranch).reversed()
        )
    val redBraintreeAlewife =
        pattern(
            "Red-3-1",
            RoutePattern.Typicality.Typical,
            100101000,
            "Alewife",
            (stopsNorthOfShuttle + stopsInShuttle + stopsBraintreeBranch).reversed()
        )
    val redAshmontJfk =
        pattern(
            "Red-A-1",
            RoutePattern.Typicality.Atypical,
            100101100,
            "JFK/UMass",
            stopsAshmontBranch.reversed()
        )
    val redBraintreeJfk =
        pattern(
            "Red-B-1",
            RoutePattern.Typicality.Atypical,
            100101150,
            "JFK/UMass",
            stopsBraintreeBranch.reversed()
        )
    val redKendallAlewife =
        pattern(
            "Red-S-1",
            RoutePattern.Typicality.Atypical,
            100101160,
            "Alewife",
            stopsNorthOfShuttle.reversed()
        )

    val patternIdsByStop =
        buildMap<String, List<String>> {
            for (pattern in objects.routePatterns.values) {
                val patternId = pattern.id
                val trip = objects.trips.getValue(pattern.representativeTripId)
                for (stopId in trip.stopIds!!) {
                    put(stopId, getOrElse(stopId, ::emptyList) + listOf(patternId))
                }
            }
        }

    // 588541
    val alert =
        objects.alert {
            effect = Alert.Effect.Shuttle
            activePeriod(
                Instant.parse("2024-08-19T04:30:00-04:00"),
                Instant.parse("2024-08-26T02:30:00-04:00")
            )
            fun informedEntity(stop: Stop, board: Boolean = true, exit: Boolean = true) =
                this.informedEntity(
                    buildList {
                        if (board) add(Alert.InformedEntity.Activity.Board)
                        if (exit) add(Alert.InformedEntity.Activity.Exit)
                        add(Alert.InformedEntity.Activity.Ride)
                    },
                    routeType = RouteType.HEAVY_RAIL,
                    route = red.id,
                    stop = stop.id
                )
            fun informedEntity(station: StopWithPlatforms) {
                informedEntity(station.southbound)
                informedEntity(station.northbound)
            }
            informedEntity(kendallMit.southbound, exit = false)
            informedEntity(kendallMit.northbound, board = false)
            informedEntity(charlesMgh)
            informedEntity(parkStreet)
            informedEntity(downtownCrossing)
            informedEntity(southStation)
            informedEntity(broadway)
            informedEntity(andrew)
            // technically, the JFK/UMass Braintree platforms had BOARD, EXIT, RIDE,
            // but I don't think that was correct, and it shouldn't matter for this test
            informedEntity(jfkUmass.southbound, board = false)
            informedEntity(jfkUmass.northbound, exit = false)
        }

    fun trip(routePattern: RoutePattern, id: String) = objects.trip(routePattern) { this.id = id }

    fun schedule(
        trip: Trip,
        stop: Stop,
        time: String,
        departs: Boolean = true,
        arrives: Boolean = true
    ) =
        objects.schedule {
            this.trip = trip
            this.stopId = stop.id
            val predictionTime = Instant.parse("2024-08-19T$time:00-04:00")
            if (departs) this.departureTime = predictionTime
            if (arrives) this.arrivalTime = predictionTime
        }

    fun prediction(trip: Trip, stop: Stop, arrivalTime: String?, departureTime: String?) =
        objects.prediction {
            this.trip = trip
            this.stopId = stop.id
            if (arrivalTime != null)
                this.arrivalTime = Instant.parse("2024-08-19T$arrivalTime-04:00")
            if (departureTime != null)
                this.departureTime = Instant.parse("2024-08-19T$departureTime-04:00")
        }

    val tripBraintreeJfk = trip(redBraintreeJfk, "BraintreeJfk")
    val scheduleBraintreeJfk =
        schedule(tripBraintreeJfk, jfkUmass.northbound, "16:45", departs = false)
    val tripKendallAlewife = trip(redKendallAlewife, "KendallAlewife")
    val scheduleKendallAlewife = schedule(tripKendallAlewife, harvard.northbound, "16:45")
    val tripJfkAshmont = trip(redJfkAshmont, "JfkAshmont")
    val scheduleJfkAshmont = schedule(tripJfkAshmont, jfkUmass.southbound, "16:49", arrives = false)
    val tripAlewifeKendall = trip(redAlewifeKendall, "AlewifeKendall")
    val scheduleAlewifeKendall = schedule(tripAlewifeKendall, harvard.southbound, "16:50")
    val tripAshmontJfk = trip(redAshmontJfk, "AshmontJfk")
    val scheduleAshmontJfk = schedule(tripAshmontJfk, jfkUmass.northbound, "16:52", departs = false)
    val tripJfkBraintree = trip(redJfkBraintree, "JfkBraintree")
    val scheduleJfkBraintree =
        schedule(tripJfkBraintree, jfkUmass.southbound, "16:56", arrives = false)

    val tripBraintreeAlewife1 = trip(redBraintreeAlewife, "BraintreeAlewife1")
    val predictionBraintreeAlewife1 =
        prediction(tripBraintreeAlewife1, harvard.northbound, "16:43:22", "16:44:30")
    val tripAshmontAlewife1 = trip(redAshmontAlewife, "AshmontAlewife1")
    val predictionAshmontAlewife1 =
        prediction(tripAshmontAlewife1, jfkUmass.northbound, "16:44:00", null)
    val tripAlewifeBraintree1 = trip(redAlewifeBraintree, "AlewifeBraintree1")
    val predictionAlewifeBraintree1 =
        prediction(tripAlewifeBraintree1, harvard.southbound, "16:50:38", "16:51:55")
    val tripAlewifeBraintree2 = trip(redAlewifeBraintree, "AlewifeBraintree2")
    val predictionAlewifeBraintree2 =
        prediction(tripAlewifeBraintree2, harvard.southbound, "16:52:58", "16:54:15")
    val tripAshmontAlewife2 = trip(redAshmontAlewife, "AshmontAlewife2")
    val predictionAshmontAlewife2 =
        prediction(tripAshmontAlewife2, jfkUmass.northbound, "16:56:11", null)
    val tripAlewifeBraintree3 = trip(redAlewifeBraintree, "AlewifeBraintree3")
    val predictionAlewifeBraintree3 =
        prediction(tripAlewifeBraintree3, harvard.southbound, "17:03:16", "17:04:33")
    val tripAshmontAlewife3 = trip(redAshmontAlewife, "AshmontAlewife3")
    val predictionAshmontAlewife3 =
        prediction(tripAshmontAlewife3, jfkUmass.northbound, "17:08:20", null)

    val globalData = GlobalResponse(objects, patternIdsByStop)
    val position = Position(latitude = 42.351371, longitude = -71.066496)
    val nearbyOutsideShuttle = NearbyResponse(harvard.childIds)
    val nearbyInsideShuttle = NearbyResponse(parkStreet.childIds)
    val nearbyAtShuttleEdge = NearbyResponse(jfkUmass.childIds)

    fun schedulesAt(stopWithPlatforms: StopWithPlatforms) =
        ScheduleResponse(
            objects.schedules.values.filter { it.stopId in stopWithPlatforms.childIds },
            objects.trips
        )

    fun predictionsAt(stopWithPlatforms: StopWithPlatforms) =
        PredictionsStreamDataResponse(
            objects.predictions.filterValues { it.stopId in stopWithPlatforms.childIds },
            objects.trips,
            objects.vehicles
        )

    val alerts = AlertsStreamDataResponse(objects)

    fun expected(stop: Stop, vararg realtimePatterns: RealtimePatterns) =
        listOf(
            StopsAssociated.WithRoute(
                red,
                listOf(PatternsByStop(red, stop, realtimePatterns.asList()))
            )
        )

    @Test
    fun `shows only regular terminals when outside shuttle`() = runBlocking {
        val expected =
            expected(
                harvard.station,
                RealtimePatterns.ByHeadsign(
                    red,
                    "Braintree",
                    null,
                    listOf(redAlewifeBraintree),
                    listOf(
                        UpcomingTrip(tripAlewifeBraintree1, predictionAlewifeBraintree1),
                        UpcomingTrip(tripAlewifeBraintree2, predictionAlewifeBraintree2),
                        UpcomingTrip(tripAlewifeBraintree3, predictionAlewifeBraintree3)
                    ),
                    emptyList()
                ),
                RealtimePatterns.ByHeadsign(
                    red,
                    "Alewife",
                    null,
                    listOf(redBraintreeAlewife, redAshmontAlewife, redKendallAlewife),
                    listOf(
                        UpcomingTrip(tripBraintreeAlewife1, predictionBraintreeAlewife1),
                        UpcomingTrip(tripKendallAlewife, scheduleKendallAlewife)
                    ),
                    emptyList()
                ),
                RealtimePatterns.ByHeadsign(
                    red,
                    "Ashmont",
                    null,
                    listOf(redAlewifeAshmont),
                    emptyList(),
                    emptyList()
                )
            )
        assertEquals(
            expected.condensed(),
            NearbyStaticData(globalData, nearbyOutsideShuttle)
                .withRealtimeInfo(
                    globalData,
                    position,
                    schedulesAt(harvard),
                    predictionsAt(harvard),
                    alerts,
                    now,
                    emptySet(),
                )!!
                .condensed()
        )
        assertEquals(
            expected.condensed(),
            StopDetailsDepartures.fromData(
                    harvard.station,
                    globalData,
                    schedulesAt(harvard),
                    predictionsAt(harvard),
                    alerts,
                    emptySet(),
                    now,
                )!!
                .asNearby()
                .condensed()
        )
    }

    @Test
    fun `shows only regular terminals when inside shuttle`() = runBlocking {
        val expected =
            expected(
                parkStreet.station,
                RealtimePatterns.ByHeadsign(
                    red,
                    "Braintree",
                    null,
                    listOf(redAlewifeBraintree),
                    emptyList(),
                    listOf(alert)
                ),
                RealtimePatterns.ByHeadsign(
                    red,
                    "Ashmont",
                    null,
                    listOf(redAlewifeAshmont),
                    emptyList(),
                    listOf(alert)
                ),
                RealtimePatterns.ByHeadsign(
                    red,
                    "Alewife",
                    null,
                    listOf(redBraintreeAlewife, redAshmontAlewife),
                    emptyList(),
                    listOf(alert)
                )
            )
        assertEquals(
            expected.condensed(),
            NearbyStaticData(globalData, nearbyInsideShuttle)
                .withRealtimeInfo(
                    globalData,
                    position,
                    schedulesAt(parkStreet),
                    predictionsAt(parkStreet),
                    alerts,
                    now,
                    emptySet(),
                )!!
                .condensed()
        )
        assertEquals(
            expected.condensed(),
            StopDetailsDepartures.fromData(
                    parkStreet.station,
                    globalData,
                    schedulesAt(parkStreet),
                    predictionsAt(parkStreet),
                    alerts,
                    emptySet(),
                    now,
                )!!
                .asNearby()
                .condensed()
        )
    }

    @Test
    fun `shows only regular terminals when at boundary of shuttle`() = runBlocking {
        val expected =
            expected(
                jfkUmass.station,
                RealtimePatterns.ByHeadsign(
                    red,
                    "Braintree",
                    null,
                    listOf(redAlewifeBraintree, redJfkBraintree),
                    listOf(UpcomingTrip(tripJfkBraintree, scheduleJfkBraintree)),
                    emptyList()
                ),
                RealtimePatterns.ByHeadsign(
                    red,
                    "Ashmont",
                    null,
                    listOf(redAlewifeAshmont, redJfkAshmont),
                    listOf(UpcomingTrip(tripJfkAshmont, scheduleJfkAshmont)),
                    emptyList()
                ),
                RealtimePatterns.ByHeadsign(
                    red,
                    "Alewife",
                    null,
                    listOf(redBraintreeAlewife, redAshmontAlewife),
                    listOf(
                        UpcomingTrip(tripAshmontAlewife1, predictionAshmontAlewife1),
                        UpcomingTrip(tripAshmontAlewife2, predictionAshmontAlewife2),
                        UpcomingTrip(tripAshmontAlewife3, predictionAshmontAlewife3)
                    ),
                    listOf(alert)
                )
            )
        assertEquals(
            expected.condensed(),
            NearbyStaticData(globalData, nearbyAtShuttleEdge)
                .withRealtimeInfo(
                    globalData,
                    position,
                    schedulesAt(jfkUmass),
                    predictionsAt(jfkUmass),
                    alerts,
                    now,
                    emptySet(),
                )!!
                .condensed()
        )
        assertEquals(
            expected.condensed(),
            StopDetailsDepartures.fromData(
                    jfkUmass.station,
                    globalData,
                    schedulesAt(jfkUmass),
                    predictionsAt(jfkUmass),
                    alerts,
                    emptySet(),
                    now,
                )!!
                .asNearby()
                .condensed()
        )
    }

    // for easier assertions about stop details
    fun StopDetailsDepartures.asNearby(): List<StopsAssociated> =
        listOf(StopsAssociated.WithRoute(red, this.routes))

    // for more legible diffs
    fun List<StopsAssociated>.condensed(): String {
        fun Instant.condensed() = toLocalDateTime(TimeZone.of("America/New_York")).time.toString()
        fun timeRangeCondensed(arrivalTime: Instant?, departureTime: Instant?) = buildString {
            if (arrivalTime != null) append(arrivalTime.condensed())
            if (arrivalTime != null && departureTime != null && arrivalTime != departureTime)
                append("-")
            if (departureTime != null && arrivalTime != departureTime)
                append(departureTime.condensed())
        }
        fun UpcomingTrip.condensed() = buildString {
            append("            trip=${trip.id}")
            schedule?.let {
                append(" schedule=")
                append(timeRangeCondensed(it.arrivalTime, it.departureTime))
            }
            prediction?.let {
                append(" prediction=")
                append(timeRangeCondensed(it.arrivalTime, it.departureTime))
            }
            if (vehicle != null) append(" vehicle=$vehicle")
        }

        fun RealtimePatterns.condensed() =
            when (this) {
                is RealtimePatterns.ByHeadsign -> "        $headsign"
                is RealtimePatterns.ByDirection ->
                    "        ${direction.name} to ${direction.destination}"
            } +
                " (patterns=${patterns.joinToString { it?.id ?: "<null>" }}) alerts=${alertsHere.orEmpty().joinToString(prefix = "[", postfix = "]") { it.id }}\n" +
                upcomingTrips.joinToString(separator = "\n") { it.condensed() }

        fun PatternsByStop.condensed() =
            "    stop=${stop.id}\n${patterns.joinToString(separator = "\n") { it.condensed() }}"

        fun StopsAssociated.condensed() =
            when (this) {
                is StopsAssociated.WithLine ->
                    "line=${line.id} (routes=${routes.joinToString { it.id }})"
                is StopsAssociated.WithRoute -> "route=${route.id}"
            } + patternsByStop.joinToString(separator = "\n", prefix = "\n") { it.condensed() }

        return joinToString(separator = "\n") { it.condensed() }
    }
}
