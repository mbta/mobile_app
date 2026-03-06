package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class AlertSummaryTest {
    @Test
    fun `can serialize and deserialize full standard summary`() {
        val jsonObject = buildJsonObject {
            put("type", "standard")
            put("effect", "station_closure")
            putJsonObject("location") {
                put("type", "single_stop")
                put("stop_name", "Lechmere")
            }
            putJsonObject("timeframe") { put("type", "tomorrow") }
            putJsonObject("recurrence") {
                put("type", "some_days")
                putJsonObject("ending") {
                    put("type", "later_date")
                    put("time", "2026-01-16T10:31:00-05:00")
                }
            }
        }
        val summary: AlertSummary =
            AlertSummary.Standard(
                Alert.Effect.StationClosure,
                AlertSummary.Location.SingleStop("Lechmere"),
                AlertSummary.Timeframe.Tomorrow,
                AlertSummary.Recurrence.SomeDays(
                    ending =
                        AlertSummary.Timeframe.LaterDate(
                            EasternTimeInstant(2026, Month.JANUARY, 16, 10, 31)
                        )
                ),
            )
        assertEquals(jsonObject, json.encodeToJsonElement(summary))
        assertEquals(summary, json.decodeFromJsonElement(jsonObject))
    }

    @Test
    fun `can serialize and deserialize all clear summary`() {
        val jsonObject = buildJsonObject {
            put("type", "all_clear")
            putJsonObject("location") {
                put("type", "successive_stops")
                put("start_stop_name", "Lechmere")
                put("end_stop_name", "Government Center")
            }
        }
        val summary: AlertSummary =
            AlertSummary.AllClear(
                AlertSummary.Location.SuccessiveStops("Lechmere", "Government Center")
            )
        assertEquals(jsonObject, json.encodeToJsonElement(summary))
        assertEquals(summary, json.decodeFromJsonElement(jsonObject))
    }

    @Test
    fun `can deserialize all locations`() {
        fun performCheck(
            location: AlertSummary.Location,
            jsonBuilder: JsonObjectBuilder.() -> Unit,
        ) {
            val jsonObject = buildJsonObject(jsonBuilder)
            assertEquals(jsonObject, json.encodeToJsonElement(location))
            assertEquals(location, json.decodeFromJsonElement(jsonObject))
        }

        performCheck(
            AlertSummary.Location.DirectionToStop(Direction("East", "Union Square", 1), "Lechmere")
        ) {
            put("type", "direction_to_stop")
            putJsonObject("direction") {
                put("name", "East")
                put("destination", "Union Square")
                put("id", 1)
            }
            put("end_stop_name", "Lechmere")
        }

        performCheck(AlertSummary.Location.SingleStop("Lechmere")) {
            put("type", "single_stop")
            put("stop_name", "Lechmere")
        }

        performCheck(
            AlertSummary.Location.StopToDirection("Lechmere", Direction("West", "Copley & West", 0))
        ) {
            put("type", "stop_to_direction")
            put("start_stop_name", "Lechmere")
            putJsonObject("direction") {
                put("name", "West")
                put("destination", "Copley & West")
                put("id", 0)
            }
        }

        performCheck(AlertSummary.Location.SuccessiveStops("Lechmere", "North Station")) {
            put("type", "successive_stops")
            put("start_stop_name", "Lechmere")
            put("end_stop_name", "North Station")
        }
    }

    @Test
    fun `can deserialize all timeframes`() {
        fun performCheck(
            timeframe: AlertSummary.Timeframe,
            jsonBuilder: JsonObjectBuilder.() -> Unit,
        ) {
            val jsonObject = buildJsonObject(jsonBuilder)
            assertEquals(jsonObject, json.encodeToJsonElement(timeframe))
            assertEquals(timeframe, json.decodeFromJsonElement(jsonObject))
        }

        performCheck(AlertSummary.Timeframe.EndOfService) { put("type", "end_of_service") }

        performCheck(AlertSummary.Timeframe.Tomorrow) { put("type", "tomorrow") }

        performCheck(
            AlertSummary.Timeframe.LaterDate(EasternTimeInstant(2025, Month.DECEMBER, 30, 16, 11))
        ) {
            put("type", "later_date")
            put("time", "2025-12-30T16:11:00-05:00")
        }

        performCheck(
            AlertSummary.Timeframe.ThisWeek(EasternTimeInstant(2025, Month.DECEMBER, 30, 16, 12))
        ) {
            put("type", "this_week")
            put("time", "2025-12-30T16:12:00-05:00")
        }

        performCheck(
            AlertSummary.Timeframe.Time(EasternTimeInstant(2025, Month.DECEMBER, 30, 16, 12))
        ) {
            put("type", "time")
            put("time", "2025-12-30T16:12:00-05:00")
        }

        performCheck(AlertSummary.Timeframe.StartingTomorrow) { put("type", "starting_tomorrow") }

        performCheck(
            AlertSummary.Timeframe.StartingLaterToday(
                EasternTimeInstant(2026, Month.JANUARY, 15, 13, 3)
            )
        ) {
            put("type", "starting_later_today")
            put("time", "2026-01-15T13:03:00-05:00")
        }

        performCheck(
            AlertSummary.Timeframe.TimeRange(
                AlertSummary.Timeframe.TimeRange.Time(
                    EasternTimeInstant(2026, Month.JANUARY, 23, 15, 35)
                ),
                AlertSummary.Timeframe.TimeRange.EndOfService,
            )
        ) {
            put("type", "time_range")
            putJsonObject("start_time") {
                put("type", "time")
                put("time", "2026-01-23T15:35:00-05:00")
            }
            putJsonObject("end_time") { put("type", "end_of_service") }
        }
    }

    @Test
    fun `can deserialize all recurrences`() {
        fun performCheck(
            recurrence: AlertSummary.Recurrence,
            jsonBuilder: JsonObjectBuilder.() -> Unit,
        ) {
            val jsonObject = buildJsonObject(jsonBuilder)
            assertEquals(jsonObject, json.encodeToJsonElement(recurrence))
            assertEquals(recurrence, json.decodeFromJsonElement(jsonObject))
        }

        performCheck(AlertSummary.Recurrence.Daily(ending = AlertSummary.Timeframe.Tomorrow)) {
            put("type", "daily")
            putJsonObject("ending") { put("type", "tomorrow") }
        }

        performCheck(
            AlertSummary.Recurrence.SomeDays(
                ending =
                    AlertSummary.Timeframe.ThisWeek(
                        EasternTimeInstant(2026, Month.JANUARY, 16, 10, 46)
                    )
            )
        ) {
            put("type", "some_days")
            putJsonObject("ending") {
                put("type", "this_week")
                put("time", "2026-01-16T10:46:00-05:00")
            }
        }
    }

    @Test
    fun `ignores unknown location timeframe and recurrence when deserializing`() {
        val jsonObject = buildJsonObject {
            put("effect", "station_closure")
            putJsonObject("location") { put("type", "omnipresent") }
            putJsonObject("timeframe") { put("type", "omnitemporal") }
            putJsonObject("recurrence") { put("type", "fortnightly") }
        }
        assertEquals(
            AlertSummary.Standard(
                Alert.Effect.StationClosure,
                location = AlertSummary.Location.Unknown,
                timeframe = AlertSummary.Timeframe.Unknown,
                recurrence = AlertSummary.Recurrence.Unknown,
            ),
            json.decodeFromJsonElement(jsonObject),
        )
    }

    @Test
    fun `ignores unknown top-level keys when deserializing`() {
        val jsonObject = buildJsonObject {
            put("effect", "station_closure")
            put("location", JsonNull)
            put("timeframe", JsonNull)
            put("hue", "octarine")
        }
        assertEquals(
            AlertSummary.Standard(Alert.Effect.StationClosure, location = null, timeframe = null),
            json.decodeFromJsonElement(jsonObject),
        )
    }

    @Test
    fun `summary is null when there is no timeframe or location`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                durationCertainty = Alert.DurationCertainty.Estimated
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertNull(alertSummary)
    }

    @Test
    fun `summary with until further notice timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), null)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.UntilFurtherNotice),
            alertSummary,
        )
    }

    @Test
    fun `summary with later today timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val endTime = now.plus(1.hours)
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.Time(endTime)),
            alertSummary,
        )
    }

    @Test
    fun `summary with end of service timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        val tomorrow = now.serviceDate.plus(DatePeriod(days = 1))
        val serviceEndTime = LocalTime(hour = 2, minute = 59)
        val endTime = EasternTimeInstant(tomorrow.atTime(serviceEndTime))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.EndOfService),
            alertSummary,
        )
    }

    @Test
    fun `summary with alt end of service timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        val tomorrow = now.serviceDate.plus(DatePeriod(days = 1))
        val serviceEndTime = LocalTime(hour = 3, minute = 0)
        val endTime = EasternTimeInstant(tomorrow.atTime(serviceEndTime))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.EndOfService),
            alertSummary,
        )
    }

    @Test
    fun `summary with tomorrow timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        // Set to tomorrow's end of service, with a date of 2 days from now
        val tomorrow = now.serviceDate.plus(DatePeriod(days = 2))
        val serviceEndTime = LocalTime(hour = 3, minute = 0)
        val endTime = EasternTimeInstant(tomorrow.atTime(serviceEndTime))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.Tomorrow),
            alertSummary,
        )
    }

    @Test
    fun `summary with this week timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        // Fixed time so we can have a specific day of the week (wed)
        val now = EasternTimeInstant(2025, Month.APRIL, 2, 9, 0)

        val saturday = now.serviceDate.plus(DatePeriod(days = 3))
        val serviceEndTime = LocalTime(hour = 5, minute = 0)
        val endTime = EasternTimeInstant(saturday.atTime(serviceEndTime))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.ThisWeek(endTime)),
            alertSummary,
        )
    }

    @Test
    fun `summary with later date timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        // Fixed time so we can have a specific day of the week (wed)
        val now = EasternTimeInstant(2025, Month.APRIL, 2, 9, 0)

        val monday = now.serviceDate.plus(DatePeriod(days = 5))
        val serviceEndTime = LocalTime(hour = 5, minute = 0)
        val endTime = EasternTimeInstant(monday.atTime(serviceEndTime))

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now.minus(1.hours), endTime)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.LaterDate(endTime)),
            alertSummary,
        )
    }

    @Test
    fun `summary with starting tomorrow timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(now + 1.days, null)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(alert.effect, null, AlertSummary.Timeframe.StartingTomorrow),
            alertSummary,
        )
    }

    @Test
    fun `summary with starting later today timeframe`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val laterToday = now + 1.hours

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                activePeriod(laterToday, null)
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                null,
                AlertSummary.Timeframe.StartingLaterToday(laterToday),
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with single stop`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val stop =
            objects.stop {
                name = "Parent Name"
                childStop {}
            }
        val childStop = objects.stops[stop.childStopIds.first()]

        val route = objects.route {}
        val pattern = objects.routePattern(route) { directionId = 0 }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                informedEntity(route = route.id.idText, stop = childStop?.id)
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "",
                0,
                listOf(pattern),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(alert.effect, AlertSummary.Location.SingleStop(stop.name), null),
            alertSummary,
        )
    }

    @Test
    fun `summary with successive stops`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val firstStop = objects.stop { name = "First Stop" }
        val successiveStops = (1..4).map { objects.stop { name = "Successive Stop $it" } }
        val lastStop = objects.stop { name = "Last Stop" }

        val stops = listOf(firstStop) + successiveStops + listOf(lastStop)

        val route = objects.route {}
        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = stops.map { it.id } }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stop in successiveStops) {
                    informedEntity(route = route.id.idText, stop = stop.id)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "",
                0,
                listOf(pattern),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.SuccessiveStops(
                    successiveStops.first().name,
                    successiveStops.last().name,
                ),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with successive bus stops`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val firstStop = objects.stop { name = "First Stop" }
        val successiveStops = (1..4).map { objects.stop { name = "Successive Stop $it" } }
        val lastStop = objects.stop { name = "Last Stop" }

        val stops = listOf(firstStop) + successiveStops + listOf(lastStop)

        val route = objects.route { type = RouteType.BUS }
        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = stops.map { it.id } }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stop in successiveStops) {
                    informedEntity(route = route.id.idText, stop = stop.id)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "",
                0,
                listOf(pattern),
                now,
                null,
                GlobalResponse(objects),
            )

        assertNull(alertSummary)
    }

    @Test
    fun `summary with branching stops ahead`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val unaffectedStops = (1..4).map { objects.stop { name = "Unaffected Stop $it" } }
        val firstStop = objects.stop { name = "First Stop" }
        val trunkStops = (1..4).map { objects.stop { name = "Successive Stop $it" } }
        val branch1Stops = (1..4).map { objects.stop { name = "Branch 1 Stop $it" } }
        val branch2Stops = (1..4).map { objects.stop { name = "Branch 2 Stop $it" } }

        val route =
            objects.route {
                directionNames = listOf("Inbound", "Outbound")
                directionDestinations = listOf("A", "Z")
            }

        val branch1 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    stopIds =
                        (unaffectedStops + listOf(firstStop) + trunkStops + branch1Stops).map {
                            it.id
                        }
                }
            }
        val branch2 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    stopIds =
                        (unaffectedStops + listOf(firstStop) + trunkStops + branch2Stops).map {
                            it.id
                        }
                }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stop in (listOf(firstStop) + trunkStops + branch1Stops + branch2Stops)) {
                    informedEntity(route = route.id.idText, stop = stop.id)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                unaffectedStops.first().id,
                0,
                listOf(branch1, branch2),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.StopToDirection(
                    firstStop.name,
                    Direction(route.directionNames[0]!!, route.directionDestinations[0]!!, 0),
                ),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with branching stops behind`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val unaffectedStops = (1..4).map { objects.stop { name = "Unaffected Stop $it" } }
        val lastStop = objects.stop { name = "Last Stop" }
        val trunkStops = (1..4).map { objects.stop { name = "Successive Stop $it" } }
        val branch1Stops = (1..4).map { objects.stop { name = "Branch 1 Stop $it" } }
        val branch2Stops = (1..4).map { objects.stop { name = "Branch 2 Stop $it" } }

        val route =
            objects.route {
                directionNames = listOf("Inbound", "Outbound")
                directionDestinations = listOf("A", "Z")
            }

        val branch1 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    stopIds =
                        (branch1Stops + trunkStops + listOf(lastStop) + unaffectedStops).map {
                            it.id
                        }
                }
            }
        val branch2 =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip {
                    stopIds =
                        (branch2Stops + trunkStops + listOf(lastStop) + unaffectedStops).map {
                            it.id
                        }
                }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stop in (listOf(lastStop) + trunkStops + branch1Stops + branch2Stops)) {
                    informedEntity(route = route.id.idText, stop = stop.id)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                unaffectedStops.first().id,
                0,
                listOf(branch1, branch2),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.DirectionToStop(
                    Direction(route.directionNames[1]!!, route.directionDestinations[1]!!, 1),
                    lastStop.name,
                ),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with branching GL stops ahead`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val firstStop =
            objects.stop {
                name = "Kenmore"
                childStop { id = "70151" }
                childStop { id = "71151" }
            }
        objects.stop {
            name = "Blandford Street"
            childStop { id = "70149" }
        }
        objects.stop {
            name = "Saint Mary's Street"
            childStop { id = "70211" }
        }

        val route =
            objects.route {
                lineId = "line-Green"
                directionNames = listOf("Westbound", "Eastbound")
                directionDestinations = listOf("", "Park St & North")
            }

        val bBranch =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = listOf("71151", "70149") }
            }
        val cBranch =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = listOf("70151", "70211") }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stop in objects.stops.values) {
                    informedEntity(route = route.id.idText, stop = stop.id)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                firstStop.id,
                0,
                listOf(bBranch, cBranch),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.StopToDirection(
                    firstStop.name,
                    Direction(route.directionNames[0]!!, route.directionDestinations[0]!!, 0),
                ),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with branching GL on branch`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val trunkStop =
            objects.stop {
                name = "Kenmore"
                childStop { id = "70150" }
            }
        val bBranchStop =
            objects.stop {
                name = "Blandford Street"
                childStop { id = "70148" }
            }
        val cBranchStop =
            objects.stop {
                name = "Saint Mary's Street"
                childStop { id = "70212" }
            }
        val cBranchTerminal =
            objects.stop {
                name = "Cleveland Circle"
                childStop { id = "70237" }
            }

        val route =
            objects.route {
                id = "Green-C"
                lineId = "line-Green"
                directionNames = listOf("Westbound", "Eastbound")
                directionDestinations = listOf("", "Park St & North")
            }

        val cBranch =
            objects.routePattern(route) {
                directionId = 1
                representativeTrip { stopIds = listOf("70237", "70212", "70150") }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stopId in listOf("70150", "70148", "70212")) {
                    informedEntity(route = route.id.idText, stop = stopId)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                cBranchStop.id,
                1,
                listOf(cBranch),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.SuccessiveStops(cBranchStop.name, trunkStop.name),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with branching GL on opposite and disconnected branch`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val eBranchStart =
            objects.stop {
                name = "Medford/Tufts"
                childStop { id = "70511" }
            }
        val eBranchEnd =
            objects.stop {
                name = "Heath Street"
                childStop { id = "70260" }
            }

        val trunkAlertingStop =
            objects.stop {
                name = "Kenmore"
                childStop { id = "70151" }
                childStop { id = "71151" }
            }
        val bStop =
            objects.stop {
                name = "Blandford Street"
                childStop { id = "70149" }
            }
        val cStop =
            objects.stop {
                name = "Saint Mary's Street"
                childStop { id = "70211" }
            }

        val route =
            objects.route {
                lineId = "line-Green"
                directionNames = listOf("Westbound", "Eastbound")
                directionDestinations = listOf("Copley & West", "Medford/Tufts")
            }

        val bBranch =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = listOf("71151", "70149") }
            }
        val cBranch =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = listOf("70151", "70211") }
            }

        val eBranch =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = listOf("70511", "70260") }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                for (stopId in
                    listOf(trunkAlertingStop, bStop, cStop).flatMap {
                        listOf(it.id) + it.childStopIds
                    }) {
                    informedEntity(route = route.id.idText, stop = stopId)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                eBranchStart.id,
                0,
                listOf(eBranch),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.StopToDirection(
                    trunkAlertingStop.name,
                    Direction(route.directionNames[0]!!, route.directionDestinations[0]!!, 0),
                ),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with daily recurrence ending on a later date`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val today = now.local.date
        val timeStart = now.local.time
        val timeEnd = (now + 1.seconds).local.time
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                for (daysForward in 0..30) {
                    val thisDay = today.plus(DatePeriod(days = daysForward))
                    activePeriod(
                        EasternTimeInstant(thisDay, timeStart),
                        EasternTimeInstant(thisDay, timeEnd),
                    )
                }
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                location = null,
                timeframe =
                    AlertSummary.Timeframe.TimeRange(
                        AlertSummary.Timeframe.TimeRange.Time(now),
                        AlertSummary.Timeframe.TimeRange.Time(now + 1.seconds),
                    ),
                recurrence =
                    AlertSummary.Recurrence.Daily(
                        ending =
                            AlertSummary.Timeframe.LaterDate(
                                EasternTimeInstant(today.plus(DatePeriod(days = 30)), timeEnd)
                            )
                    ),
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with daily recurrence until further notice`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()
        val today = now.local.date
        val timeStart = now.local.time
        val timeEnd = (now + 1.seconds).local.time
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                durationCertainty = Alert.DurationCertainty.Unknown
                for (daysForward in 0..30) {
                    val thisDay = today.plus(DatePeriod(days = daysForward))
                    activePeriod(
                        EasternTimeInstant(thisDay, timeStart),
                        EasternTimeInstant(thisDay, timeEnd),
                    )
                }
            }

        val alertSummary =
            AlertSummary.summarizing(alert, "", 0, emptyList(), now, null, GlobalResponse(objects))

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                location = null,
                timeframe =
                    AlertSummary.Timeframe.TimeRange(
                        AlertSummary.Timeframe.TimeRange.Time(now),
                        AlertSummary.Timeframe.TimeRange.Time(now + 1.seconds),
                    ),
                recurrence =
                    AlertSummary.Recurrence.Daily(
                        ending = AlertSummary.Timeframe.UntilFurtherNotice
                    ),
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with MWF recurrence ending later this week`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val monday = LocalDate(2026, Month.JANUARY, 12)
        val tuesday = LocalDate(2026, Month.JANUARY, 13)
        val wednesday = LocalDate(2026, Month.JANUARY, 14)
        val thursday = LocalDate(2026, Month.JANUARY, 15)
        val friday = LocalDate(2026, Month.JANUARY, 16)
        val saturday = LocalDate(2026, Month.JANUARY, 17)
        val serviceBoundary = LocalTime(3, 0)
        val noon = LocalTime(12, 0)
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod(
                    EasternTimeInstant(monday, serviceBoundary),
                    EasternTimeInstant(tuesday, serviceBoundary),
                )
                activePeriod(
                    EasternTimeInstant(wednesday, serviceBoundary),
                    EasternTimeInstant(thursday, serviceBoundary),
                )
                activePeriod(
                    EasternTimeInstant(friday, serviceBoundary),
                    EasternTimeInstant(saturday, serviceBoundary),
                )
            }

        val expectedRecurrence =
            AlertSummary.Recurrence.SomeDays(
                ending =
                    AlertSummary.Timeframe.ThisWeek(EasternTimeInstant(saturday, serviceBoundary))
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                location = null,
                timeframe =
                    AlertSummary.Timeframe.TimeRange(
                        AlertSummary.Timeframe.TimeRange.StartOfService,
                        AlertSummary.Timeframe.TimeRange.EndOfService,
                    ),
                recurrence = expectedRecurrence,
            ),
            AlertSummary.summarizing(
                alert,
                "",
                0,
                emptyList(),
                EasternTimeInstant(monday, noon),
                null,
                GlobalResponse(objects),
            ),
        )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                location = null,
                timeframe = AlertSummary.Timeframe.StartingTomorrow,
                recurrence = expectedRecurrence,
            ),
            AlertSummary.summarizing(
                alert,
                "",
                0,
                emptyList(),
                EasternTimeInstant(tuesday, noon),
                null,
                GlobalResponse(objects),
            ),
        )
    }

    @Test
    fun `summary with active update does not display update message`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        val firstStop = objects.stop { name = "First Stop" }
        val successiveStops = (1..4).map { objects.stop { name = "Successive Stop $it" } }
        val lastStop = objects.stop { name = "Last Stop" }

        val stops = listOf(firstStop) + successiveStops + listOf(lastStop)

        val route = objects.route {}
        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = stops.map { it.id } }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), now.plus(1.hours))
                updatedAt = now.minus(1.minutes)
                for (stop in successiveStops) {
                    informedEntity(route = route.id.idText, stop = stop.id)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "",
                0,
                listOf(pattern),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.SuccessiveStops(
                    successiveStops.first().name,
                    successiveStops.last().name,
                ),
                null,
                null,
                isUpdate = false,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with all clear update`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        val firstStop = objects.stop { name = "First Stop" }
        val successiveStops = (1..4).map { objects.stop { name = "Successive Stop $it" } }
        val lastStop = objects.stop { name = "Last Stop" }

        val stops = listOf(firstStop) + successiveStops + listOf(lastStop)

        val route = objects.route {}
        val pattern =
            objects.routePattern(route) {
                directionId = 0
                representativeTrip { stopIds = stops.map { it.id } }
            }
        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                activePeriod(now.minus(1.hours), now.minus(5.minutes))
                for (stop in successiveStops) {
                    informedEntity(route = route.id.idText, stop = stop.id)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "",
                0,
                listOf(pattern),
                now,
                null,
                GlobalResponse(objects),
            )

        assertTrue { alert.allClear(now) }
        assertEquals(
            AlertSummary.AllClear(
                AlertSummary.Location.SuccessiveStops(
                    successiveStops.first().name,
                    successiveStops.last().name,
                )
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with whole route entity`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        val route = objects.route { longName = "Route Label" }
        val pattern = objects.routePattern(route) {}

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                durationCertainty = Alert.DurationCertainty.Unknown
                activePeriod(now.minus(1.hours), null)
                informedEntity(route = route.id.idText, routeType = route.type)
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "",
                0,
                listOf(pattern),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                location = AlertSummary.Location.WholeRoute(route.label, route.type),
                timeframe = AlertSummary.Timeframe.UntilFurtherNotice,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with stop entities for every stop on a route`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val now = EasternTimeInstant.now()

        val route = objects.route { longName = "Route Label" }
        val stopIds = listOf("1", "2", "3", "4")
        stopIds.forEach { objects.stop { id = it } }
        val trip = objects.trip { this.stopIds = stopIds }
        val pattern =
            objects.routePattern(route) {
                typicality = RoutePattern.Typicality.Typical
                representativeTripId = trip.id
            }

        val alert =
            objects.alert {
                effect = Alert.Effect.Suspension
                durationCertainty = Alert.DurationCertainty.Unknown
                activePeriod(now.minus(1.hours), null)
                stopIds.forEach {
                    informedEntity(route = route.id.idText, routeType = route.type, stop = it)
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "",
                0,
                listOf(pattern),
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                location = AlertSummary.Location.WholeRoute(route.label, route.type),
                timeframe = AlertSummary.Timeframe.UntilFurtherNotice,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with whole green line alert`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val routes =
            greenRoutes.map {
                objects.route {
                    id = it.toString()
                    type = RouteType.LIGHT_RAIL
                    lineId = "line-Green"
                }
            }
        val patterns =
            routes.map { objects.routePattern(it) { typicality = RoutePattern.Typicality.Typical } }

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), null)
                routes.forEach { informedEntity(route = it.id.idText, routeType = it.type) }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "stopId",
                0,
                patterns.filter { it.routeId.idText == "Green-E" },
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.WholeRoute("Green Line", RouteType.LIGHT_RAIL),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with stop entities for every stop on the green line`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val routes =
            greenRoutes.map {
                objects.route {
                    id = it.toString()
                    type = RouteType.LIGHT_RAIL
                    lineId = "line-Green"
                }
            }
        val stops: Map<String, List<Stop>> =
            (greenRoutes.map { it.idText } + "trunk").associateWith { route ->
                listOf("1", "2", "3", "4").map { objects.stop { id = "$route$it" } }
            }
        val patterns =
            routes.map {
                val stopList: List<Stop> =
                    (stops[it.id.idText] ?: emptyList()) + (stops["trunk"] ?: emptyList())
                objects.routePattern(it) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { stopIds = stopList.map { stop -> stop.id } }
                }
            }

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), null)
                stops.forEach { (id, stops) ->
                    if (id == "trunk") {
                        routes.forEach { route ->
                            stops.forEach { stop ->
                                informedEntity(route = route.id.idText, stop = stop.id)
                            }
                        }
                    } else {
                        stops.forEach { stop -> informedEntity(route = id, stop = stop.id) }
                    }
                }
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "stopId",
                0,
                patterns.filter { it.routeId.idText == "Green-E" },
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.WholeRoute("Green Line", RouteType.LIGHT_RAIL),
                null,
            ),
            alertSummary,
        )
    }

    @Test
    fun `summary with stop entities for single green line branch`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val now = EasternTimeInstant.now()

        val routes =
            greenRoutes.map {
                objects.route {
                    id = it.toString()
                    longName = it.toString()
                    type = RouteType.LIGHT_RAIL
                    lineId = "line-Green"
                }
            }

        val patterns =
            routes.map { objects.routePattern(it) { typicality = RoutePattern.Typicality.Typical } }

        val alert =
            objects.alert {
                effect = Alert.Effect.StopClosure
                durationCertainty = Alert.DurationCertainty.Estimated
                activePeriod(now.minus(1.hours), null)

                informedEntity(route = "Green-C", routeType = RouteType.LIGHT_RAIL)
            }

        val alertSummary =
            AlertSummary.summarizing(
                alert,
                "stopId",
                0,
                patterns.filter { it.routeId.idText == "Green-E" },
                now,
                null,
                GlobalResponse(objects),
            )

        assertEquals(
            AlertSummary.Standard(
                alert.effect,
                AlertSummary.Location.WholeRoute("Green-C", RouteType.LIGHT_RAIL),
                null,
            ),
            alertSummary,
        )
    }
}
