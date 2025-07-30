package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.response.NearbyResponse
import com.mbta.tid.mbta_app.model.response.PredictionsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.ScheduleResponse
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Month

class RouteCardDataTest {

    @Test
    fun `ListBuilder addStaticStopsData when there are no new patterns for a stop then it is omitted`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()

            val route1 = objects.route()

            val route1rp1 =
                objects.routePattern(route1) {
                    representativeTrip {
                        headsign = "Harvard"
                        directionId = 0
                    }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(stop1.id to listOf(route1rp1.id), stop2.id to listOf(route1rp1.id)),
                )
            val nearby = NearbyResponse(objects)
            val context = RouteCardData.Context.NearbyTransit
            val now = EasternTimeInstant.now()

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                mapOf(
                    route1.id to
                        RouteCardData.Builder(
                            lineOrRoute1,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop1,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop1,
                                                    directionId = 0,
                                                    routePatterns = listOf(route1rp1),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        )
                ),
                RouteCardData.ListBuilder(false, context, now)
                    .addStaticStopsData(nearby.stopIds, global, context, null)
                    .data,
            )
        }

    @Test
    fun `ListBuilder addStaticStopsData when second stop serves a new rp for route served by previous stop then include all rps at second stop`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()

            val route1 = objects.route()

            val route1rp1 =
                objects.routePattern(route1) {
                    representativeTrip {
                        headsign = "Harvard"
                        directionId = 0
                    }
                }
            val route1rp2 =
                objects.routePattern(route1) {
                    representativeTrip {
                        headsign = "Nubian"
                        directionId = 0
                    }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to listOf(route1rp1.id),
                            stop2.id to listOf(route1rp1.id, route1rp2.id),
                        ),
                )
            val nearby = NearbyResponse(objects)
            val context = RouteCardData.Context.NearbyTransit
            val now = EasternTimeInstant.now()

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                mapOf(
                    route1.id to
                        RouteCardData.Builder(
                            lineOrRoute1,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop1,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop1,
                                                    directionId = 0,
                                                    routePatterns = listOf(route1rp1),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = true,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    ),
                                stop2.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop2,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop2,
                                                    directionId = 0,
                                                    routePatterns = listOf(route1rp1, route1rp2),
                                                    patternsNotSeenAtEarlierStops =
                                                        setOf(route1rp2.id),
                                                    stopIds = setOf(stop2.id),
                                                    allDataLoaded = true,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    ),
                            ),
                            now,
                        )
                ),
                RouteCardData.ListBuilder(true, context, now)
                    .addStaticStopsData(nearby.stopIds, global, context, null)
                    .data,
            )
        }

    @Test
    fun `ListBuilder addStaticStopsData groups patterns by direction`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        val route1rp1 =
            objects.routePattern(route1) {
                representativeTrip { headsign = "Harvard" }
                directionId = 0
            }
        val route1rp2 =
            objects.routePattern(route1) {
                representativeTrip { headsign = "Harvard v2" }
                directionId = 0
            }
        val route1rp3 =
            objects.routePattern(route1) {
                representativeTrip { headsign = "Nubian" }
                directionId = 1
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(stop1.id to listOf(route1rp1.id, route1rp2.id, route1rp3.id)),
            )
        val nearby = NearbyResponse(objects)
        val context = RouteCardData.Context.NearbyTransit
        val now = EasternTimeInstant.now()

        val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
        assertEquals(
            mapOf(
                route1.id to
                    RouteCardData.Builder(
                        lineOrRoute1,
                        mapOf(
                            stop1.id to
                                RouteCardData.RouteStopDataBuilder(
                                    route1,
                                    stop1,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute1,
                                                stop = stop1,
                                                directionId = 0,
                                                routePatterns = listOf(route1rp1, route1rp2),
                                                stopIds = setOf(stop1.id),
                                                allDataLoaded = false,
                                                context = context,
                                            ),
                                        1 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute1,
                                                stop = stop1,
                                                directionId = 1,
                                                routePatterns = listOf(route1rp3),
                                                stopIds = setOf(stop1.id),
                                                allDataLoaded = false,
                                                context = context,
                                            ),
                                    ),
                                    global,
                                )
                        ),
                        now,
                    )
            ),
            RouteCardData.ListBuilder(false, context, now)
                .addStaticStopsData(nearby.stopIds, global, context, null)
                .data,
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData filters for only favorites when context is favorites`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()
            val stop3 = objects.stop()

            val route1 = objects.route()
            val route2 = objects.route()

            val route1rp1 =
                objects.routePattern(route1) {
                    representativeTrip { headsign = "Harvard" }
                    directionId = 0
                }
            val route1rp2 =
                objects.routePattern(route1) {
                    representativeTrip { headsign = "Harvard v2" }
                    directionId = 0
                }
            val route1rp3 =
                objects.routePattern(route1) {
                    representativeTrip { headsign = "Nubian" }
                    directionId = 1
                }

            val route2rp1 =
                objects.routePattern(route2) {
                    representativeTrip { headsign = "Harvard" }
                    directionId = 0
                }
            val route2rp2 =
                objects.routePattern(route2) {
                    representativeTrip { headsign = "Harvard v2" }
                    directionId = 0
                }
            val route2rp3 =
                objects.routePattern(route2) {
                    representativeTrip { headsign = "Alewife" }
                    directionId = 1
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to
                                listOf(
                                    route1rp1.id,
                                    route1rp2.id,
                                    route1rp3.id,
                                    route2rp1.id,
                                    route2rp2.id,
                                    route2rp3.id,
                                ),
                            stop2.id to listOf(route2rp1.id, route2rp2.id, route2rp3.id),
                            stop3.id to listOf(route1rp1.id, route1rp2.id, route1rp3.id),
                        ),
                )
            val context = RouteCardData.Context.Favorites
            val favorites =
                setOf(
                    RouteStopDirection(route1.id, stop3.id, 0),
                    RouteStopDirection(route2.id, stop1.id, 1),
                )
            val now = EasternTimeInstant.now()

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            val lineOrRoute2 = RouteCardData.LineOrRoute.Route(route2)
            assertEquals(
                mapOf(
                    route2.id to
                        RouteCardData.Builder(
                            lineOrRoute2,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route2,
                                        stop1,
                                        mapOf(
                                            1 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute2,
                                                    stop = stop1,
                                                    directionId = 1,
                                                    routePatterns = listOf(route2rp3),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        ),
                    route1.id to
                        RouteCardData.Builder(
                            lineOrRoute1,
                            mapOf(
                                stop3.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop3,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop3,
                                                    directionId = 0,
                                                    routePatterns = listOf(route1rp1, route1rp2),
                                                    stopIds = setOf(stop3.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        ),
                ),
                RouteCardData.ListBuilder(false, context, now)
                    .addStaticStopsData(
                        listOf(stop1.id, stop2.id, stop3.id),
                        global,
                        context,
                        favorites,
                    )
                    .data,
            )
        }

    @Test
    fun `ListBuilder addStaticStopsData does not filter for favorites when context is not favorites`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()
            val stop3 = objects.stop()

            val route1 = objects.route()
            val route2 = objects.route()

            val route1rp1 =
                objects.routePattern(route1) {
                    representativeTrip { headsign = "Harvard" }
                    directionId = 0
                }
            val route1rp2 =
                objects.routePattern(route1) {
                    representativeTrip { headsign = "Harvard v2" }
                    directionId = 0
                }
            val route1rp3 =
                objects.routePattern(route1) {
                    representativeTrip { headsign = "Nubian" }
                    directionId = 1
                }

            val route2rp1 =
                objects.routePattern(route2) {
                    representativeTrip { headsign = "Harvard" }
                    directionId = 0
                }
            val route2rp2 =
                objects.routePattern(route2) {
                    representativeTrip { headsign = "Harvard v2" }
                    directionId = 0
                }
            val route2rp3 =
                objects.routePattern(route2) {
                    representativeTrip { headsign = "Alewife" }
                    directionId = 1
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to
                                listOf(
                                    route1rp1.id,
                                    route1rp2.id,
                                    route1rp3.id,
                                    route2rp1.id,
                                    route2rp2.id,
                                    route2rp3.id,
                                ),
                            stop2.id to listOf(route2rp1.id, route2rp2.id, route2rp3.id),
                            stop3.id to listOf(route1rp1.id, route1rp2.id, route1rp3.id),
                        ),
                )
            val context = RouteCardData.Context.NearbyTransit
            val favorites =
                setOf(
                    RouteStopDirection(route1.id, stop3.id, 0),
                    RouteStopDirection(route2.id, stop1.id, 1),
                )
            val now = EasternTimeInstant.now()

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            val lineOrRoute2 = RouteCardData.LineOrRoute.Route(route2)
            assertEquals(
                mapOf(
                    route1.id to
                        RouteCardData.Builder(
                            lineOrRoute1,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop1,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop1,
                                                    directionId = 0,
                                                    routePatterns = listOf(route1rp1, route1rp2),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                ),
                                            1 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop1,
                                                    directionId = 1,
                                                    routePatterns = listOf(route1rp3),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                ),
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        ),
                    route2.id to
                        RouteCardData.Builder(
                            lineOrRoute2,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route2,
                                        stop1,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute2,
                                                    stop = stop1,
                                                    directionId = 0,
                                                    routePatterns = listOf(route2rp1, route2rp2),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                ),
                                            1 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute2,
                                                    stop = stop1,
                                                    directionId = 1,
                                                    routePatterns = listOf(route2rp3),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                ),
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        ),
                ),
                RouteCardData.ListBuilder(false, context, now)
                    .addStaticStopsData(
                        listOf(stop1.id, stop2.id, stop3.id),
                        global,
                        context,
                        favorites,
                    )
                    .data,
            )
        }

    @Test
    fun `ListBuilder addStaticStopsData when a stop is served by multiple routes it is included for each route`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()

            val route1 = objects.route()
            val route2 = objects.route()

            val route1rp1 =
                objects.routePattern(route1) {
                    representativeTrip { headsign = "Harvard" }
                    directionId = 0
                }
            val route2rp1 =
                objects.routePattern(route2) {
                    representativeTrip { headsign = "Kenmore" }
                    directionId = 0
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop = mapOf(stop1.id to listOf(route1rp1.id, route2rp1.id)),
                )
            val nearby = NearbyResponse(objects)
            val context = RouteCardData.Context.NearbyTransit
            val now = EasternTimeInstant.now()

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            val lineOrRoute2 = RouteCardData.LineOrRoute.Route(route2)
            assertEquals(
                mapOf(
                    route1.id to
                        RouteCardData.Builder(
                            lineOrRoute1,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop1,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop1,
                                                    directionId = 0,
                                                    routePatterns = listOf(route1rp1),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        ),
                    route2.id to
                        RouteCardData.Builder(
                            lineOrRoute2,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route2,
                                        stop1,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute2,
                                                    stop = stop1,
                                                    directionId = 0,
                                                    routePatterns = listOf(route2rp1),
                                                    stopIds = setOf(stop1.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        ),
                ),
                RouteCardData.ListBuilder(false, context, now)
                    .addStaticStopsData(nearby.stopIds, global, context, null)
                    .data,
            )
        }

    @Test
    fun `ListBuilder addStaticStopsData groups by parent station`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val station1 = objects.stop { id = "station_1" }

        val station1stop1 =
            objects.stop {
                parentStationId = station1.id
                id = "stop_1"
            }
        val station1stop2 =
            objects.stop {
                parentStationId = station1.id
                id = "stop_2"
            }

        val stop2 = objects.stop()

        val route1 = objects.route()

        val route1rp1 =
            objects.routePattern(route1) {
                sortOrder = 10
                representativeTrip { headsign = "Harvard" }
            }
        val route1rp2 =
            objects.routePattern(route1) {
                sortOrder = 11
                representativeTrip { headsign = "Nubian" }
            }
        val route1rp3 =
            objects.routePattern(route1) {
                sortOrder = 12
                representativeTrip { headsign = "Nubian via Allston" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        station1stop1.id to listOf(route1rp1.id),
                        station1stop2.id to listOf(route1rp2.id),
                        stop2.id to listOf(route1rp3.id),
                    ),
            )
        val nearby = NearbyResponse(objects)
        val context = RouteCardData.Context.NearbyTransit
        val now = EasternTimeInstant.now()

        val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
        assertEquals(
            mapOf(
                route1.id to
                    RouteCardData.Builder(
                        lineOrRoute1,
                        mapOf(
                            station1.id to
                                RouteCardData.RouteStopDataBuilder(
                                    route1,
                                    station1,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute1,
                                                stop = station1,
                                                directionId = 0,
                                                routePatterns = listOf(route1rp1, route1rp2),
                                                stopIds =
                                                    setOf(
                                                        station1.id,
                                                        station1stop1.id,
                                                        station1stop2.id,
                                                    ),
                                                allDataLoaded = false,
                                                context = context,
                                            )
                                    ),
                                    global,
                                ),
                            stop2.id to
                                RouteCardData.RouteStopDataBuilder(
                                    route1,
                                    stop2,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute1,
                                                stop = stop2,
                                                directionId = 0,
                                                routePatterns = listOf(route1rp3),
                                                stopIds = setOf(stop2.id),
                                                allDataLoaded = false,
                                                context = context,
                                            )
                                    ),
                                    global,
                                ),
                        ),
                        now,
                    )
            ),
            RouteCardData.ListBuilder(false, context, now)
                .addStaticStopsData(nearby.stopIds, global, context, null)
                .data,
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData preserves unscheduled physical platform`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val parentStation = objects.stop { id = "place-forhl" }

        val logicalPlatform =
            objects.stop {
                id = "70001"
                parentStationId = parentStation.id
            }
        val physicalPlatform =
            objects.stop {
                id = "Forest Hills-01"
                parentStationId = parentStation.id
            }

        val route = objects.route { id = "Orange" }

        val routePattern =
            objects.routePattern(route) { representativeTrip { headsign = "Oak Grove" } }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop = mapOf(logicalPlatform.id to listOf(routePattern.id)),
            )
        val nearby = NearbyResponse(objects)
        val context = RouteCardData.Context.NearbyTransit
        val now = EasternTimeInstant.now()

        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
        assertEquals(
            mapOf(
                route.id to
                    RouteCardData.Builder(
                        lineOrRoute,
                        mapOf(
                            parentStation.id to
                                RouteCardData.RouteStopDataBuilder(
                                    route,
                                    parentStation,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute,
                                                stop = parentStation,
                                                directionId = 0,
                                                routePatterns = listOf(routePattern),
                                                stopIds =
                                                    setOf(
                                                        parentStation.id,
                                                        logicalPlatform.id,
                                                        physicalPlatform.id,
                                                    ),
                                                allDataLoaded = false,
                                                context = context,
                                            )
                                    ),
                                    global,
                                )
                        ),
                        now,
                    )
            ),
            RouteCardData.ListBuilder(false, context, now)
                .addStaticStopsData(nearby.stopIds, global, context, null)
                .data,
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData Green Line shuttles are not grouped together`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop = objects.stop()

            val line =
                objects.line {
                    id = "line-Green"
                    sortOrder = 0
                }

            val railRoute =
                objects.route {
                    lineId = line.id
                    directionNames = listOf("West", "East")
                    directionDestinations = listOf("Boston College", "Government Center")
                }
            val shuttleRoute =
                objects.route {
                    id = "Shuttle-$id"
                    lineId = line.id
                }

            val railPattern =
                objects.routePattern(railRoute) {
                    representativeTrip {
                        headsign = "Boston College"
                        typicality = RoutePattern.Typicality.Typical
                    }
                }
            val shuttlePattern =
                objects.routePattern(shuttleRoute) {
                    representativeTrip { headsign = "Boston College" }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop = mapOf(stop.id to listOf(railPattern.id, shuttlePattern.id)),
                )
            val nearby = NearbyResponse(objects)
            val context = RouteCardData.Context.NearbyTransit
            val now = EasternTimeInstant.now()

            val westDir = Direction("West", "Boston College", 0)
            val eastDir = Direction("East", "Government Center", 1)

            val railLineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(railRoute))
            val shuttleLineOrRoute = RouteCardData.LineOrRoute.Route(shuttleRoute)
            assertEquals(
                mapOf(
                    line.id to
                        RouteCardData.Builder(
                            railLineOrRoute,
                            mapOf(
                                stop.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        railLineOrRoute,
                                        stop,
                                        listOf(westDir, eastDir),
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = railLineOrRoute,
                                                    stop = stop,
                                                    directionId = 0,
                                                    routePatterns = listOf(railPattern),
                                                    stopIds = setOf(stop.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                    )
                            ),
                            now,
                        ),
                    shuttleRoute.id to
                        RouteCardData.Builder(
                            shuttleLineOrRoute,
                            mapOf(
                                stop.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        shuttleRoute,
                                        stop,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = shuttleLineOrRoute,
                                                    stop = stop,
                                                    directionId = 0,
                                                    routePatterns = listOf(shuttlePattern),
                                                    stopIds = setOf(stop.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    )
                            ),
                            now,
                        ),
                ),
                RouteCardData.ListBuilder(false, context, now)
                    .addStaticStopsData(nearby.stopIds, global, context, null)
                    .data,
            )
        }

    @Test
    fun `ListBuilder addStaticStopsData Green Line routes all grouped by direction when different child stops`() {
        val objects = ObjectCollectionBuilder()

        val parkSt = objects.stop { locationType = LocationType.STATION }

        val bWestPlatform = objects.stop { parentStationId = parkSt.id }
        val cWestPlatform = objects.stop { parentStationId = parkSt.id }
        val dWestPlatform = objects.stop { parentStationId = parkSt.id }
        val eastPlatform = objects.stop { parentStationId = parkSt.id }

        val line =
            objects.line {
                id = "line-Green"
                sortOrder = 0
            }

        val bRoute =
            objects.route {
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Boston College", "Government Center")
            }

        val cRoute =
            objects.route {
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Cleveland Circle", "Government Center")
            }

        val dRoute =
            objects.route {
                lineId = line.id
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Riverside", "Union Sq")
            }

        val bWestPattern =
            objects.routePattern(bRoute) { typicality = RoutePattern.Typicality.Typical }
        val bEastPattern =
            objects.routePattern(bRoute) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val cWestPattern =
            objects.routePattern(cRoute) { typicality = RoutePattern.Typicality.Typical }
        val cEastPattern =
            objects.routePattern(cRoute) {
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val dWestPattern =
            objects.routePattern(dRoute) { typicality = RoutePattern.Typicality.Typical }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        eastPlatform.id to listOf(bEastPattern.id, cEastPattern.id),
                        bWestPlatform.id to listOf(bWestPattern.id),
                        cWestPlatform.id to listOf(cWestPattern.id),
                        dWestPlatform.id to listOf(dWestPattern.id),
                    ),
            )
        val nearby = NearbyResponse(objects)
        val context = RouteCardData.Context.NearbyTransit
        val now = EasternTimeInstant.now()

        val lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(bRoute, cRoute, dRoute))
        assertEquals(
            mapOf(
                line.id to
                    RouteCardData.Builder(
                        lineOrRoute,
                        mapOf(
                            parkSt.id to
                                RouteCardData.RouteStopDataBuilder(
                                    line,
                                    setOf(bRoute, cRoute, dRoute),
                                    parkSt,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute,
                                                stop = parkSt,
                                                directionId = 0,
                                                routePatterns =
                                                    listOf(
                                                        bWestPattern,
                                                        cWestPattern,
                                                        dWestPattern,
                                                    ),
                                                stopIds =
                                                    setOf(
                                                        parkSt.id,
                                                        eastPlatform.id,
                                                        bWestPlatform.id,
                                                        cWestPlatform.id,
                                                        dWestPlatform.id,
                                                    ),
                                                allDataLoaded = false,
                                                context = context,
                                            ),
                                        1 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute,
                                                stop = parkSt,
                                                directionId = 1,
                                                routePatterns = listOf(bEastPattern, cEastPattern),
                                                stopIds =
                                                    setOf(
                                                        parkSt.id,
                                                        eastPlatform.id,
                                                        bWestPlatform.id,
                                                        cWestPlatform.id,
                                                        dWestPlatform.id,
                                                    ),
                                                allDataLoaded = false,
                                                context = context,
                                            ),
                                    ),
                                    global,
                                )
                        ),
                        now,
                    )
            ),
            RouteCardData.ListBuilder(false, context, now)
                .addStaticStopsData(nearby.stopIds, global, context, null)
                .data,
        )
    }

    @Test
    fun `ListBuilder addStaticStopsData Green Line routes are grouped together without Government Center direction`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stopGov = objects.stop { id = "place-gover" }
            // Only get nearby results at Government Center
            val nearby = NearbyResponse(objects)

            // These stops are included because they're thresholds in Direction.specialCases
            val stopArlington = objects.stop { id = "place-armnl" }
            val stopHaymarket = objects.stop { id = "place-haecl" }

            val stop1 = objects.stop { id = "place-lake" }
            val stop2 = objects.stop { id = "place-clmnl" }
            val stop3 = objects.stop { id = "place-river" }
            val stop4 = objects.stop { id = "place-pktrm" }
            val stop5 = objects.stop { id = "place-mgngl" }

            val line = objects.line { id = "line-Green" }

            val routeB =
                objects.route {
                    id = "Green-B"
                    lineId = line.id
                    directionNames = listOf("West", "East")
                    directionDestinations = listOf("Boston College", "Government Center")
                }
            val routeC =
                objects.route {
                    id = "Green-C"
                    lineId = line.id
                    directionNames = listOf("West", "East")
                    directionDestinations = listOf("Cleveland Circle", "Government Center")
                }
            val routeD =
                objects.route {
                    id = "Green-D"
                    lineId = line.id
                    directionNames = listOf("West", "East")
                    directionDestinations = listOf("Riverside", "Union Square")
                }

            val routeBrp1 =
                objects.routePattern(routeB) {
                    representativeTrip {
                        headsign = "Boston College"
                        stopIds = listOf(stopGov.id, stop4.id, stopArlington.id, stop1.id)
                    }
                    id = "routeBrp1"
                    directionId = 0
                    sortOrder = 3
                    typicality = RoutePattern.Typicality.Typical
                }
            val routeBrp2 =
                objects.routePattern(routeB) {
                    representativeTrip {
                        headsign = "Government Center"
                        stopIds = listOf(stop1.id, stopArlington.id, stop4.id, stopGov.id)
                    }
                    id = "routeBrp2"
                    directionId = 1
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Typical
                }
            val routeCrp1 =
                objects.routePattern(routeC) {
                    representativeTrip {
                        headsign = "Cleveland Circle"
                        stopIds = listOf(stopGov.id, stop4.id, stopArlington.id, stop2.id)
                    }
                    id = "routeCrp1"
                    directionId = 0
                    sortOrder = 3
                    typicality = RoutePattern.Typicality.Typical
                }
            val routeCrp2 =
                objects.routePattern(routeC) {
                    representativeTrip {
                        headsign = "Government Center"
                        stopIds = listOf(stop2.id, stopArlington.id, stop4.id, stopGov.id)
                    }
                    id = "routeCrp2"
                    directionId = 1
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Typical
                }
            val routeCrp3 =
                objects.routePattern(routeC) {
                    representativeTrip {
                        headsign = "Union Square"
                        stopIds =
                            listOf(
                                stop2.id,
                                stopArlington.id,
                                stop4.id,
                                stopGov.id,
                                stopHaymarket.id,
                                stop5.id,
                            )
                    }
                    id = "routeCrp3"
                    directionId = 1
                    sortOrder = 5
                    typicality = RoutePattern.Typicality.Atypical
                }
            val routeDrp1 =
                objects.routePattern(routeD) {
                    representativeTrip {
                        headsign = "Riverside"
                        stopIds =
                            listOf(
                                stop5.id,
                                stopHaymarket.id,
                                stopGov.id,
                                stop4.id,
                                stopArlington.id,
                                stop3.id,
                            )
                    }
                    id = "routeDrp1"
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                }
            val routeDrp2 =
                objects.routePattern(routeD) {
                    representativeTrip {
                        headsign = "Union Square"
                        stopIds =
                            listOf(
                                stop3.id,
                                stopArlington.id,
                                stop4.id,
                                stopGov.id,
                                stopHaymarket.id,
                                stop5.id,
                            )
                    }
                    id = "routeDrp2"
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 1
                    sortOrder = 2
                }

            val global = GlobalResponse(objects)
            val context = RouteCardData.Context.NearbyTransit
            val now = EasternTimeInstant.now()

            val westDir = Direction("West", "Copley & West", 0)
            val northDir = Direction("East", "North Station & North", 1)

            val lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(routeB, routeC, routeD))
            assertEquals(
                mapOf(
                    line.id to
                        RouteCardData.Builder(
                            lineOrRoute,
                            mapOf(
                                stopGov.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        lineOrRoute,
                                        stopGov,
                                        listOf(westDir, northDir),
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute,
                                                    stop = stopGov,
                                                    directionId = 0,
                                                    routePatterns =
                                                        listOf(routeBrp1, routeCrp1, routeDrp1),
                                                    stopIds = setOf(stopGov.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                ),
                                            1 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute,
                                                    stop = stopGov,
                                                    directionId = 1,
                                                    routePatterns =
                                                        listOf(
                                                            routeBrp2,
                                                            routeCrp2,
                                                            routeCrp3,
                                                            routeDrp2,
                                                        ),
                                                    stopIds = setOf(stopGov.id),
                                                    allDataLoaded = false,
                                                    context = context,
                                                ),
                                        ),
                                    )
                            ),
                            now,
                        )
                ),
                RouteCardData.ListBuilder(false, context, now)
                    .addStaticStopsData(nearby.stopIds, global, context, null)
                    .data,
            )
        }

    @Test
    fun `ListBuilder addUpcomingTrips includes predictions filtered to the correct stop and direction`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()

            val route1 = objects.route()

            val pattern1 =
                objects.routePattern(route1) {
                    sortOrder = 1
                    representativeTrip { headsign = "Harvard" }
                }
            val trip1 = objects.trip(pattern1)
            val pattern2 =
                objects.routePattern(route1) {
                    sortOrder = 2
                    representativeTrip { headsign = "Harvard" }
                }
            val trip2 = objects.trip(pattern2)
            val pattern3 =
                objects.routePattern(route1) {
                    sortOrder = 3
                    representativeTrip { headsign = "Nubian" }
                }
            val trip3 = objects.trip(pattern3)

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to listOf(pattern1.id, pattern2.id),
                            stop2.id to listOf(pattern3.id),
                        ),
                )
            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 21, 9, 30, 8)

            // should be sorted before the pattern 1 prediction under Harvard
            val stop1Pattern2Prediction =
                objects.prediction {
                    arrivalTime = time
                    departureTime = time + 10.seconds
                    stopId = stop1.id
                    trip = trip2
                }

            // should be sorted after the pattern 2 prediction under Harvard
            val stop1Pattern1Prediction =
                objects.prediction {
                    arrivalTime = time + 5.seconds
                    departureTime = time + 15.seconds
                    stopId = stop1.id
                    trip = trip1
                }

            // should *not* be ignored since pattern 1 shows at stop, but pattern 3 does not
            val stop2Pattern1Prediction =
                objects.prediction {
                    arrivalTime = time + 10.seconds
                    departureTime = time + 20.seconds
                    stopId = stop2.id
                    trip = trip1
                }

            // should be shown under Nubian
            val stop2Pattern3Prediction =
                objects.prediction {
                    arrivalTime = time + 20.seconds
                    departureTime = time + 30.seconds
                    stopId = stop2.id
                    trip = trip3
                }

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                mapOf(
                    route1.id to
                        RouteCardData.Builder(
                            lineOrRoute1,
                            mapOf(
                                stop1.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop1,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop1,
                                                    directionId = 0,
                                                    routePatterns = listOf(pattern1, pattern2),
                                                    stopIds = setOf(stop1.id),
                                                    upcomingTrips =
                                                        listOf(
                                                            objects.upcomingTrip(
                                                                stop1Pattern2Prediction
                                                            ),
                                                            objects.upcomingTrip(
                                                                stop1Pattern1Prediction
                                                            ),
                                                        ),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    ),
                                stop2.id to
                                    RouteCardData.RouteStopDataBuilder(
                                        route1,
                                        stop2,
                                        mapOf(
                                            0 to
                                                RouteCardData.LeafBuilder(
                                                    lineOrRoute = lineOrRoute1,
                                                    stop = stop2,
                                                    directionId = 0,
                                                    routePatterns = listOf(pattern3),
                                                    stopIds = setOf(stop2.id),
                                                    upcomingTrips =
                                                        listOf(
                                                            objects.upcomingTrip(
                                                                stop2Pattern1Prediction
                                                            ),
                                                            objects.upcomingTrip(
                                                                stop2Pattern3Prediction
                                                            ),
                                                        ),
                                                    allDataLoaded = false,
                                                    context = context,
                                                )
                                        ),
                                        global,
                                    ),
                            ),
                            time,
                        )
                ),
                RouteCardData.ListBuilder(true, context, time)
                    .addStaticStopsData(listOf(stop1.id, stop2.id), global, context, null)
                    .addUpcomingTrips(
                        null,
                        PredictionsStreamDataResponse(objects),
                        filterAtTime = time,
                        globalData = global,
                    )
                    .data,
            )
        }

    @Test
    fun `ListBuilder addUpcomingTrips sets hasSchedulesToday per leaf`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()
        val stop2 = objects.stop()
        val stop3 = objects.stop()

        val route = objects.route { type = RouteType.HEAVY_RAIL }

        val pattern1 =
            objects.routePattern(route) {
                sortOrder = 1
                representativeTrip { headsign = "Future Schedule" }
            }
        val pattern2 =
            objects.routePattern(route) {
                sortOrder = 2
                representativeTrip { headsign = "Past Schedule" }
            }
        val pattern3 =
            objects.routePattern(route) {
                sortOrder = 3
                representativeTrip { headsign = "No Schedule" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        stop1.id to listOf(pattern1.id),
                        stop2.id to listOf(pattern2.id),
                        stop3.id to listOf(pattern3.id),
                    ),
            )
        val context = RouteCardData.Context.NearbyTransit
        val now = EasternTimeInstant.now()

        val futureSchedule =
            objects.schedule {
                trip = objects.trip(pattern1)
                stopId = stop1.id
                departureTime = now + 2.hours
            }
        objects.schedule {
            trip = objects.trip(pattern2)
            stopId = stop2.id
            departureTime = now - 2.hours
        }

        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
        assertEquals(
            mapOf(
                route.id to
                    RouteCardData.Builder(
                        lineOrRoute,
                        mapOf(
                            stop1.id to
                                RouteCardData.RouteStopDataBuilder(
                                    route,
                                    stop1,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute,
                                                stop = stop1,
                                                directionId = 0,
                                                routePatterns = listOf(pattern1),
                                                stopIds = setOf(stop1.id),
                                                upcomingTrips =
                                                    listOf(objects.upcomingTrip(futureSchedule)),
                                                allDataLoaded = true,
                                                hasSchedulesTodayByPattern =
                                                    mapOf(pattern1.id to true),
                                                context = context,
                                            )
                                    ),
                                    global,
                                ),
                            stop2.id to
                                RouteCardData.RouteStopDataBuilder(
                                    route,
                                    stop2,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute,
                                                stop = stop2,
                                                directionId = 0,
                                                routePatterns = listOf(pattern2),
                                                stopIds = setOf(stop2.id),
                                                upcomingTrips = null,
                                                allDataLoaded = true,
                                                hasSchedulesTodayByPattern =
                                                    mapOf(pattern2.id to true),
                                                context = context,
                                            )
                                    ),
                                    global,
                                ),
                            stop3.id to
                                RouteCardData.RouteStopDataBuilder(
                                    route,
                                    stop3,
                                    mapOf(
                                        0 to
                                            RouteCardData.LeafBuilder(
                                                lineOrRoute = lineOrRoute,
                                                stop = stop3,
                                                directionId = 0,
                                                routePatterns = listOf(pattern3),
                                                stopIds = setOf(stop3.id),
                                                upcomingTrips = null,
                                                allDataLoaded = true,
                                                hasSchedulesTodayByPattern =
                                                    mapOf(pattern3.id to false),
                                                context = context,
                                            )
                                    ),
                                    global,
                                ),
                        ),
                        now,
                    )
            ),
            RouteCardData.ListBuilder(true, context, now)
                .addStaticStopsData(listOf(stop1.id, stop2.id, stop3.id), global, context, null)
                .addUpcomingTrips(
                    ScheduleResponse(objects),
                    PredictionsStreamDataResponse(objects),
                    filterAtTime = now,
                    globalData = global,
                )
                .data,
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList sorts subway routes first`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val busStop = objects.stop()
        val subwayStop = objects.stop()

        val subwayRoute = objects.route { type = RouteType.LIGHT_RAIL }
        val busRoute = objects.route { type = RouteType.BUS }

        val subwayRp =
            objects.routePattern(subwayRoute) {
                sortOrder = 2
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Alewife" }
            }
        val busRp =
            objects.routePattern(busRoute) {
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical

                representativeTrip { headsign = "Nubian" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        subwayStop.id to listOf(subwayRp.id, busRp.id),
                        busStop.id to listOf(busRp.id),
                    ),
            )

        val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)
        val context = RouteCardData.Context.NearbyTransit

        val subwayLineOrRoute = RouteCardData.LineOrRoute.Route(subwayRoute)
        val busLineOrRoute = RouteCardData.LineOrRoute.Route(busRoute)
        assertEquals(
            listOf(
                RouteCardData(
                    lineOrRoute = subwayLineOrRoute,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                subwayRoute,
                                subwayStop,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = subwayLineOrRoute,
                                        stop = subwayStop,
                                        directionId = 0,
                                        routePatterns = listOf(subwayRp),
                                        stopIds = setOf(subwayStop.id),
                                        upcomingTrips = listOf(),
                                        alertsHere = emptyList(),
                                        allDataLoaded = false,
                                        hasSchedulesToday = false,
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    )
                                ),
                                global,
                            )
                        ),
                    time,
                ),
                RouteCardData(
                    lineOrRoute = busLineOrRoute,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                busRoute,
                                busStop,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = busLineOrRoute,
                                        stop = busStop,
                                        directionId = 0,
                                        routePatterns = listOf(busRp),
                                        stopIds = setOf(busStop.id),
                                        upcomingTrips = listOf(),
                                        alertsHere = emptyList(),
                                        allDataLoaded = false,
                                        hasSchedulesToday = false,
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    )
                                ),
                                global,
                            )
                        ),
                    time,
                ),
            ),
            RouteCardData.routeCardsForStopList(
                listOf(busStop.id, subwayStop.id),
                global,
                sortByDistanceFrom = null,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                now = time,
                pinnedRoutes = setOf(),
                context = context,
            ),
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList preserves original stop ordering among subway routes`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val closerStop = objects.stop()
            val furtherStop = objects.stop()

            val subwayRoute1 = objects.route { type = RouteType.LIGHT_RAIL }
            val subwayRoute2 = objects.route { type = RouteType.LIGHT_RAIL }

            val subway1Rp1 =
                objects.routePattern(subwayRoute1) {
                    sortOrder = 2
                    representativeTrip { headsign = "Alewife" }
                    typicality = RoutePattern.Typicality.Typical
                }
            val subway2Rp1 =
                objects.routePattern(subwayRoute2) {
                    sortOrder = 1
                    representativeTrip { headsign = "Braintree" }
                    typicality = RoutePattern.Typicality.Typical
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            closerStop.id to listOf(subway2Rp1.id),
                            furtherStop.id to listOf(subway1Rp1.id),
                        ),
                )
            val nearby = NearbyResponse(objects)
            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val subwayLineOrRoute1 = RouteCardData.LineOrRoute.Route(subwayRoute1)
            val subwayLineOrRoute2 = RouteCardData.LineOrRoute.Route(subwayRoute2)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = subwayLineOrRoute2,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    subwayRoute2,
                                    closerStop,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = subwayLineOrRoute2,
                                            stop = closerStop,
                                            directionId = 0,
                                            routePatterns = listOf(subway2Rp1),
                                            stopIds = setOf(closerStop.id),
                                            upcomingTrips = listOf(),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    ),
                    RouteCardData(
                        lineOrRoute = subwayLineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    subwayRoute1,
                                    furtherStop,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = subwayLineOrRoute1,
                                            stop = furtherStop,
                                            directionId = 0,
                                            routePatterns = listOf(subway1Rp1),
                                            stopIds = setOf(furtherStop.id),
                                            upcomingTrips = listOf(),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    ),
                ),
                RouteCardData.routeCardsForStopList(
                    nearby.stopIds,
                    global,
                    sortByDistanceFrom = null,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList sorts subway first then by distance`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val closeBusStop = objects.stop()
        val farBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.1
                longitude = closeBusStop.longitude + 0.1
            }
        val closeSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.2
                longitude = closeBusStop.longitude + 0.2
            }
        val farSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.3
                longitude = closeBusStop.longitude + 0.3
            }

        val closeBusRoute =
            objects.route {
                type = RouteType.BUS
                sortOrder = 4
            }
        val farBusRoute =
            objects.route {
                type = RouteType.BUS
                sortOrder = 3
            }
        val closeSubwayRoute =
            objects.route {
                type = RouteType.LIGHT_RAIL
                sortOrder = 2
            }
        val farSubwayRoute =
            objects.route {
                type = RouteType.LIGHT_RAIL
                sortOrder = 1
            }

        val closeSubwayPattern =
            objects.routePattern(closeSubwayRoute) { representativeTrip { headsign = "Alewife" } }
        val farSubwayPattern =
            objects.routePattern(farSubwayRoute) { representativeTrip { headsign = "Oak Grove" } }
        val closeBusPattern =
            objects.routePattern(closeBusRoute) { representativeTrip { headsign = "Nubian" } }
        val farBusPattern =
            objects.routePattern(farBusRoute) { representativeTrip { headsign = "Malden Center" } }

        val time = EasternTimeInstant(2024, Month.FEBRUARY, 21, 9, 30, 8)

        // close subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeSubwayRoute.id
            stopId = closeSubwayStop.id
            tripId = closeSubwayPattern.representativeTripId
        }

        // far subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farSubwayRoute.id
            stopId = farSubwayStop.id
            tripId = farSubwayPattern.representativeTripId
        }

        // close bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeBusRoute.id
            stopId = closeBusStop.id
            tripId = closeBusPattern.representativeTripId
        }

        // far bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farBusRoute.id
            stopId = farBusStop.id
            tripId = farBusPattern.representativeTripId
        }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        farBusStop.id to listOf(farBusPattern.id),
                        closeBusStop.id to listOf(closeBusPattern.id),
                        farSubwayStop.id to listOf(farSubwayPattern.id),
                        closeSubwayStop.id to listOf(closeSubwayPattern.id),
                    ),
            )

        val routeCardsSorted =
            RouteCardData.routeCardsForStopList(
                listOf(farBusStop.id, farSubwayStop.id, closeBusStop.id, closeSubwayStop.id),
                global,
                sortByDistanceFrom = closeBusStop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                now = time,
                pinnedRoutes = setOf(),
                context = RouteCardData.Context.NearbyTransit,
            )

        assertEquals(
            listOf(closeSubwayRoute, farSubwayRoute, closeBusRoute, farBusRoute),
            routeCardsSorted?.flatMap { it.lineOrRoute.allRoutes },
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList sorts pinned routes to the top`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val closeBusStop = objects.stop()
        val farBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.1
                longitude = closeBusStop.longitude + 0.1
            }
        val closeSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.2
                longitude = closeBusStop.longitude + 0.2
            }
        val farSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.3
                longitude = closeBusStop.longitude + 0.3
            }

        val closeBusRoute = objects.route { type = RouteType.BUS }
        val farBusRoute = objects.route { type = RouteType.BUS }
        val closeSubwayRoute = objects.route { type = RouteType.LIGHT_RAIL }
        val farSubwayRoute = objects.route { type = RouteType.LIGHT_RAIL }

        val closeSubwayPattern =
            objects.routePattern(closeSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Alewife" }
            }
        val farSubwayPattern =
            objects.routePattern(farSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Oak Grove" }
            }
        val closeBusPattern =
            objects.routePattern(closeBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Nubian" }
            }
        val farBusPattern =
            objects.routePattern(farBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Malden Center" }
            }

        val time = EasternTimeInstant(2024, Month.FEBRUARY, 21, 9, 30, 8)

        // close subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeSubwayRoute.id
            stopId = closeSubwayStop.id
            tripId = closeSubwayPattern.representativeTripId
        }

        // far subway prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farSubwayRoute.id
            stopId = farSubwayStop.id
            tripId = farSubwayPattern.representativeTripId
        }

        // close bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = closeBusRoute.id
            stopId = closeBusStop.id
            tripId = closeBusPattern.representativeTripId
        }

        // far bus prediction
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = farBusRoute.id
            stopId = farBusStop.id
            tripId = farBusPattern.representativeTripId
        }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        farBusStop.id to listOf(farBusPattern.id),
                        closeBusStop.id to listOf(closeBusPattern.id),
                        farSubwayStop.id to listOf(farSubwayPattern.id),
                        closeSubwayStop.id to listOf(closeSubwayPattern.id),
                    ),
            )

        val routeCardsSorted =
            RouteCardData.routeCardsForStopList(
                listOf(farBusStop.id, farSubwayStop.id, closeBusStop.id, closeSubwayStop.id),
                global,
                sortByDistanceFrom = closeBusStop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                now = time,
                pinnedRoutes = setOf(farBusRoute.id, farSubwayRoute.id),
                context = RouteCardData.Context.NearbyTransit,
            )

        assertEquals(
            listOf(farSubwayRoute, farBusRoute, closeSubwayRoute, closeBusRoute),
            routeCardsSorted?.flatMap { it.lineOrRoute.allRoutes },
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList sorts routes with no service today to the bottom`():
        Unit = runBlocking {
        val objects = ObjectCollectionBuilder()

        val closeBusStop = objects.stop()
        val midBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.2
                longitude = closeBusStop.longitude + 0.2
            }
        val farBusStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.4
                longitude = closeBusStop.longitude + 0.4
            }
        val closeSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.1
                longitude = closeBusStop.longitude + 0.1
            }
        val midSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.3
                longitude = closeBusStop.longitude + 0.3
            }
        val farSubwayStop =
            objects.stop {
                latitude = closeBusStop.latitude + 0.5
                longitude = closeBusStop.longitude + 0.5
            }

        // Unpinned, no schedules
        val closeBusRoute =
            objects.route {
                id = "close-bus"
                type = RouteType.BUS
            }
        // Unpinned, with schedules
        val midBusRoute =
            objects.route {
                id = "mid-bus"
                type = RouteType.BUS
            }
        // Unpinned, no schedules
        val farBusRoute =
            objects.route {
                id = "far-bus"
                type = RouteType.BUS
            }
        // Unpinned, no schedules
        val closeSubwayRoute =
            objects.route {
                id = "close-subway"
                type = RouteType.HEAVY_RAIL
            }
        // Pinned, no schedules
        val midSubwayRoute =
            objects.route {
                id = "mid-subway"
                type = RouteType.LIGHT_RAIL
            }
        // Pinned, with schedules
        val farSubwayRoute =
            objects.route {
                id = "far-subway"
                type = RouteType.HEAVY_RAIL
            }

        val closeBusPattern =
            objects.routePattern(closeBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Lincoln Lab" }
                typicality = RoutePattern.Typicality.Typical
            }
        val midBusPattern1 =
            objects.routePattern(midBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Nubian" }
                typicality = RoutePattern.Typicality.Typical
            }
        val midBusPattern2 =
            objects.routePattern(midBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Nubian" }
                typicality = RoutePattern.Typicality.Typical
            }
        val farBusPattern1 =
            objects.routePattern(farBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Malden Center" }
                typicality = RoutePattern.Typicality.Typical
            }
        val farBusPattern2 =
            objects.routePattern(farBusRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Malden Center" }
                typicality = RoutePattern.Typicality.Atypical
            }
        val closeSubwayPattern =
            objects.routePattern(closeSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Alewife" }
                typicality = RoutePattern.Typicality.Typical
            }
        val midSubwayPattern =
            objects.routePattern(midSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Medford/Tufts" }
                typicality = RoutePattern.Typicality.Typical
            }
        val farSubwayPattern =
            objects.routePattern(farSubwayRoute) {
                sortOrder = 1
                representativeTrip { headsign = "Oak Grove" }
                typicality = RoutePattern.Typicality.Typical
            }

        val time = EasternTimeInstant(2024, Month.FEBRUARY, 21, 9, 30, 8)
        objects.prediction {
            arrivalTime = time
            departureTime = time
            routeId = midBusRoute.id
            stopId = midBusStop.id
            tripId = midBusPattern1.representativeTripId
        }

        objects.schedule {
            routeId = midBusRoute.id
            tripId = midBusPattern1.representativeTripId
        }

        objects.schedule {
            routeId = farSubwayRoute.id
            tripId = farSubwayPattern.representativeTripId
        }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        farBusStop.id to listOf(farBusPattern1.id, farBusPattern2.id),
                        midBusStop.id to listOf(midBusPattern1.id, midBusPattern2.id),
                        closeBusStop.id to listOf(closeBusPattern.id),
                        farSubwayStop.id to listOf(farSubwayPattern.id),
                        midSubwayStop.id to listOf(midSubwayPattern.id),
                        closeSubwayStop.id to listOf(closeSubwayPattern.id),
                    ),
            )

        val routeCardsSorted =
            RouteCardData.routeCardsForStopList(
                listOf(
                    farBusStop.id,
                    farSubwayStop.id,
                    midSubwayStop.id,
                    closeBusStop.id,
                    midBusStop.id,
                    closeSubwayStop.id,
                ),
                global,
                sortByDistanceFrom = closeBusStop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                now = time,
                pinnedRoutes = setOf(midSubwayRoute.id, farSubwayRoute.id),
                context = RouteCardData.Context.NearbyTransit,
            )

        // Routes with no service today should sort below all routes with any service today,
        // unless they are a pinned route, in which case we want them to sort beneath all other
        // pinned routes, but above any unpinned ones. Here, the far and mid subway routes are
        // both pinned, but mid has no scheduled service, so it's sorted below the farther
        // pinned route. For unpinned routes, mid bus is the only one with any schedules, so
        // it's sorted above all the other unpinned routes, then the remaining  are ordered with
        // the usual nearby transit sort order, subway first, then by distance.
        assertEquals(
            listOf(
                farSubwayRoute,
                midSubwayRoute,
                midBusRoute,
                closeSubwayRoute,
                closeBusRoute,
                farBusRoute,
            ),
            checkNotNull(routeCardsSorted).flatMap { it.lineOrRoute.allRoutes },
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList sorts stops in route card with no service today to the bottom`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val closeBusStop = objects.stop { id = "close-bus" }
            val midBusStop =
                objects.stop {
                    id = "mid-bus"
                    latitude = closeBusStop.latitude + 0.2
                    longitude = closeBusStop.longitude + 0.2
                }
            val farBusStop =
                objects.stop {
                    id = "far-bus"
                    latitude = closeBusStop.latitude + 0.4
                    longitude = closeBusStop.longitude + 0.4
                }
            val closeSubwayStop =
                objects.stop {
                    id = "close-subway"
                    latitude = closeBusStop.latitude + 0.1
                    longitude = closeBusStop.longitude + 0.1
                }
            val midSubwayStop =
                objects.stop {
                    id = "mid-subway"
                    latitude = closeBusStop.latitude + 0.3
                    longitude = closeBusStop.longitude + 0.3
                }
            val farSubwayStop =
                objects.stop {
                    id = "far-subway"
                    latitude = closeBusStop.latitude + 0.5
                    longitude = closeBusStop.longitude + 0.5
                }

            // With predictions
            val predictedBusRoute =
                objects.route {
                    id = "predicted-bus"
                    type = RouteType.BUS
                }
            // With no upcoming schedules or predictions
            val serviceEndedBusRoute =
                objects.route {
                    id = "bus-service-ended"
                    type = RouteType.BUS
                }
            // With schedules
            val scheduledSubwayRoute =
                objects.route {
                    id = "scheduled-subway"
                    type = RouteType.HEAVY_RAIL
                }
            // With predictions
            val predictedSubwayRoute =
                objects.route {
                    id = "predicted-subway"
                    type = RouteType.LIGHT_RAIL
                }

            val predictedBusPattern =
                objects.routePattern(predictedBusRoute) {
                    sortOrder = 1
                    representativeTrip {
                        headsign = "Design Center via South Station"
                        stopIds = listOf(farBusStop.id)
                    }
                    typicality = RoutePattern.Typicality.Typical
                }
            val partialServiceEndedBusPattern =
                objects.routePattern(predictedBusRoute) {
                    sortOrder = 1
                    representativeTrip {
                        headsign = "Design Center"
                        stopIds = listOf(midBusStop.id)
                    }
                    typicality = RoutePattern.Typicality.Typical
                }
            val allServiceEndedBusPattern =
                objects.routePattern(serviceEndedBusRoute) {
                    sortOrder = 1
                    representativeTrip {
                        headsign = "Nubian"
                        stopIds = listOf(closeBusStop.id, midBusStop.id)
                    }
                    typicality = RoutePattern.Typicality.Typical
                }
            val scheduledSubwayPattern =
                objects.routePattern(scheduledSubwayRoute) {
                    sortOrder = 1
                    representativeTrip {
                        headsign = "Cleveland Circle"
                        stopIds = listOf(closeSubwayStop.id)
                    }
                    typicality = RoutePattern.Typicality.Typical
                }
            val predictedSubwayPattern =
                objects.routePattern(predictedSubwayRoute) {
                    sortOrder = 1
                    representativeTrip {
                        headsign = "Government Center"
                        stopIds = listOf(midSubwayStop.id, farSubwayStop.id)
                    }
                    typicality = RoutePattern.Typicality.Typical
                }

            val time = EasternTimeInstant(2024, Month.FEBRUARY, 21, 9, 30, 8)

            objects.prediction {
                arrivalTime = time.plus(10.minutes)
                departureTime = time.plus(10.minutes)
                routeId = predictedBusRoute.id
                stopId = farBusStop.id
                tripId = predictedBusPattern.representativeTripId
            }

            objects.prediction {
                arrivalTime = time.plus(5.minutes)
                departureTime = time.plus(5.minutes)
                routeId = predictedSubwayRoute.id
                stopId = farSubwayStop.id
                tripId = predictedSubwayPattern.representativeTripId
            }

            objects.schedule {
                departureTime = time.plus(1.hours)
                routeId = predictedBusRoute.id
                tripId = predictedBusPattern.representativeTripId
                stopId = farBusStop.id
            }

            objects.schedule {
                departureTime = time.minus(1.hours)
                routeId = predictedBusRoute.id
                tripId = partialServiceEndedBusPattern.representativeTripId
                stopId = midBusStop.id
            }

            objects.schedule {
                departureTime = time.minus(1.hours)
                routeId = serviceEndedBusRoute.id
                tripId = allServiceEndedBusPattern.representativeTripId
                stopId = closeBusStop.id
            }

            objects.schedule {
                departureTime = time.plus(1.hours)
                routeId = predictedSubwayRoute.id
                tripId = predictedSubwayPattern.representativeTripId
                stopId = farSubwayStop.id
            }

            objects.schedule {
                departureTime = time.plus(1.hours)
                routeId = scheduledSubwayRoute.id
                tripId = scheduledSubwayPattern.representativeTripId
                stopId = closeSubwayStop.id
            }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            farBusStop.id to listOf(predictedBusPattern.id),
                            midBusStop.id to
                                listOf(
                                    allServiceEndedBusPattern.id,
                                    partialServiceEndedBusPattern.id,
                                ),
                            closeBusStop.id to listOf(allServiceEndedBusPattern.id),
                            farSubwayStop.id to listOf(predictedSubwayPattern.id),
                            midSubwayStop.id to listOf(predictedSubwayPattern.id),
                            closeSubwayStop.id to listOf(scheduledSubwayPattern.id),
                        ),
                )

            val routeCardsSorted =
                RouteCardData.routeCardsForStopList(
                    listOf(
                        farBusStop.id,
                        farSubwayStop.id,
                        midSubwayStop.id,
                        closeBusStop.id,
                        midBusStop.id,
                        closeSubwayStop.id,
                    ),
                    global,
                    sortByDistanceFrom = closeBusStop.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = emptySet(),
                    context = RouteCardData.Context.NearbyTransit,
                )

            // Routes that have multiple variants where some nearby stops can have no more service
            // today while a slightly farther stop still has upcoming service should never be
            // sorted all the way to the bottom of the route list, but if all service for a route
            // at all nearby stops is done for the day, then it should be sorted to the bottom.
            assertEquals(
                listOf(
                    scheduledSubwayRoute,
                    predictedSubwayRoute,
                    predictedBusRoute,
                    serviceEndedBusRoute,
                ),
                checkNotNull(routeCardsSorted).flatMap { it.lineOrRoute.allRoutes },
            )

            val partialServiceEndedRoute =
                routeCardsSorted.find { it.lineOrRoute.sortRoute.id == predictedBusRoute.id }
            assertEquals(
                listOf(farBusStop.id, midBusStop.id),
                partialServiceEndedRoute?.stopData?.map { it.stop.id } ?: emptyList(),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList hides rare direction with no predictions in next 120 min`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()

            val route1 = objects.route()

            // should be included because typical and has prediction
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }
            // should not be included because not typical and prediction beyond 120 minutes
            val deviationInbound =
                objects.routePattern(route1) {
                    directionId = 1
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip { headsign = "Deviation In" }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(stop1.id to listOf(typicalOutbound.id, deviationInbound.id)),
                )

            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val typicalOutboundPrediction =
                objects.prediction {
                    departureTime = time
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = typicalOutbound.representativeTripId
                }

            val deviationInboundPrediction =
                objects.prediction {
                    departureTime = time + 121.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = deviationInbound.representativeTripId
                }
            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(typicalOutboundPrediction)
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    listOf(stop1.id),
                    global,
                    sortByDistanceFrom = null,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList shows rare direction with predictions in next 120 min`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()

            val route1 = objects.route()

            // should be included because typical and has prediction < 120 minutes
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }
            // should also be included even though > 120 minutes because above prediction < 120
            val deviationInbound =
                objects.routePattern(route1) {
                    directionId = 1
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip { headsign = "Deviation In" }
                }

            val deviationInboundTrip2 =
                objects.trip {
                    directionId = 1
                    routePatternId = deviationInbound.id
                    routeId = route1.id
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(stop1.id to listOf(typicalOutbound.id, deviationInbound.id)),
                )
            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val typicalOutboundPrediction =
                objects.prediction {
                    departureTime = time
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = typicalOutbound.representativeTripId
                }

            val deviationInboundPrediction =
                objects.prediction {
                    departureTime = time + 119.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = deviationInbound.representativeTripId
                }

            val deviationInboundPredictionLater =
                objects.prediction {
                    departureTime = time + 121.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = deviationInboundTrip2.id
                }
            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(typicalOutboundPrediction)
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 1,
                                            routePatterns = listOf(deviationInbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        deviationInboundPrediction
                                                    ),
                                                    objects.upcomingTrip(
                                                        deviationInboundPredictionLater
                                                    ),
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    listOf(stop1.id),
                    global,
                    sortByDistanceFrom = null,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList hides stop that has upcoming service all seen at earlier stop`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()

            val route1 = objects.route()

            // should be included because typical and has prediction
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }
            // should not be included because not typical and prediction beyond 120 minutes
            val deviationOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip { headsign = "Deviation In" }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to listOf(typicalOutbound.id),
                            stop2.id to listOf(typicalOutbound.id, deviationOutbound.id),
                        ),
                )

            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val typicalOutboundPredictionStop1 =
                objects.prediction {
                    departureTime = time
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = typicalOutbound.representativeTripId
                }

            val typicalOutboundPredictionStop2 =
                objects.prediction {
                    departureTime = time + 121.minutes
                    routeId = route1.id
                    stopId = stop2.id
                    tripId = typicalOutbound.representativeTripId
                }
            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        typicalOutboundPredictionStop1
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    listOf(stop1.id),
                    global,
                    sortByDistanceFrom = null,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList shows stop that has nontypical service not seen at earlier stop`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()

            val route1 = objects.route()

            // should be included because typical and has prediction
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }
            // should be included because not typical and not seen at stop 1
            val deviationOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip { headsign = "Deviation Out" }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to listOf(typicalOutbound.id),
                            stop2.id to listOf(typicalOutbound.id, deviationOutbound.id),
                        ),
                )

            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val typicalOutboundPredictionStop1 =
                objects.prediction {
                    departureTime = time
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = typicalOutbound.representativeTripId
                }

            val typicalOutboundPredictionStop2 =
                objects.prediction {
                    departureTime = time + 118.minutes
                    routeId = route1.id
                    stopId = stop2.id
                    tripId = typicalOutbound.representativeTripId
                }

            val deviationOutboundPredictionStop2 =
                objects.prediction {
                    departureTime = time + 100.minutes
                    routeId = route1.id
                    stopId = stop2.id
                    tripId = deviationOutbound.representativeTripId
                }
            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        typicalOutboundPredictionStop1
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                ),
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop2,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop2,
                                            directionId = 0,
                                            routePatterns =
                                                listOf(typicalOutbound, deviationOutbound),
                                            stopIds = setOf(stop2.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        deviationOutboundPredictionStop2
                                                    ),
                                                    objects.upcomingTrip(
                                                        typicalOutboundPredictionStop2
                                                    ),
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                ),
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    listOf(stop1.id, stop2.id),
                    global,
                    sortByDistanceFrom = null,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList hides stop that has nontypical service not seen at earlier stop when nontypical service won't be shown`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()

            val route1 = objects.route()

            // should be included because typical and has prediction
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }
            // should not be included because not typical and more than 3 trips in the future
            val deviationOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip { headsign = "Deviation Out" }
                }

            fun makeTripFrom(trip: Trip, id: String): Trip {
                val newTrip = trip.copy(id)
                objects.trips[id] = newTrip
                return newTrip
            }
            val typicalOutboundTrip = objects.trips[typicalOutbound.representativeTripId]!!
            fun makeTypicalTrip(id: String) = makeTripFrom(typicalOutboundTrip, id)
            val deviationOutboundTrip = objects.trips[deviationOutbound.representativeTripId]!!
            fun makeDeviationTrip(id: String) = makeTripFrom(deviationOutboundTrip, id)

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to listOf(typicalOutbound.id),
                            stop2.id to listOf(typicalOutbound.id, deviationOutbound.id),
                        ),
                )

            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val trip1 = makeTypicalTrip("trip1")
            val trip2 = makeTypicalTrip("trip2")
            val trip3 = makeTypicalTrip("trip3")
            val trip4 = makeDeviationTrip("trip4")

            val typicalOutboundPrediction1Stop1 =
                objects.prediction {
                    departureTime = time + 5.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = trip1.id
                }

            val typicalOutboundPrediction2Stop1 =
                objects.prediction {
                    departureTime = time + 15.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = trip2.id
                }

            val typicalOutboundPrediction3Stop1 =
                objects.prediction {
                    departureTime = time + 30.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = trip3.id
                }

            val typicalOutboundPrediction1Stop2 =
                objects.prediction {
                    departureTime = time + 2.minutes
                    routeId = route1.id
                    stopId = stop2.id
                    tripId = trip1.id
                }

            val typicalOutboundPrediction2Stop2 =
                objects.prediction {
                    departureTime = time + 12.minutes
                    routeId = route1.id
                    stopId = stop2.id
                    tripId = trip2.id
                }

            val typicalOutboundPrediction3Stop2 =
                objects.prediction {
                    departureTime = time + 27.minutes
                    routeId = route1.id
                    stopId = stop2.id
                    tripId = trip3.id
                }

            val deviationOutboundPredictionStop2 =
                objects.prediction {
                    departureTime = time + 40.minutes
                    routeId = route1.id
                    stopId = stop2.id
                    tripId = trip4.id
                }
            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        typicalOutboundPrediction1Stop1
                                                    ),
                                                    objects.upcomingTrip(
                                                        typicalOutboundPrediction2Stop1
                                                    ),
                                                    objects.upcomingTrip(
                                                        typicalOutboundPrediction3Stop1
                                                    ),
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    listOf(stop1.id, stop2.id),
                    global,
                    sortByDistanceFrom = null,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList shows stop that has typical service not seen at earlier stop beyond cutoff`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()
            val stop2 = objects.stop()

            val route1 = objects.route()

            // should be included because not typical but has upcoming trip
            val deviationOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip { headsign = "Deviation Out" }
                }

            // should be included because typical and not seen at stop 1
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to listOf(deviationOutbound.id),
                            stop2.id to listOf(typicalOutbound.id),
                        ),
                )

            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val deviationOutboundPredictionStop1 =
                objects.prediction {
                    departureTime = time + 100.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = deviationOutbound.representativeTripId
                }
            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(deviationOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        deviationOutboundPredictionStop1
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                ),
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop2,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop2,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop2.id),
                                            upcomingTrips = listOf(),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                ),
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    listOf(stop1.id, stop2.id),
                    global,
                    sortByDistanceFrom = null,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList handles schedule and predictions edge cases`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()

            val route1 = objects.route()
            val route2 = objects.route()
            val route3 = objects.route()
            val route4 = objects.route()

            // assigning each route pattern to a unique route + direction to more easily test
            // filtering with one upcoming trip for each

            // exclude, schedule in past
            val schedulePast =
                objects.routePattern(route1) {
                    sortOrder = 1
                    representativeTrip { headsign = "Schedule Past" }
                }
            // include, schedule upcoming
            val scheduleSoon =
                objects.routePattern(route1) {
                    directionId = 1
                    sortOrder = 2
                    representativeTrip { headsign = "Schedule Soon" }
                }
            // exclude, schedule too late
            val scheduleLater =
                objects.routePattern(route2) {
                    sortOrder = 3
                    representativeTrip { headsign = "Schedule Later" }
                }
            // exclude, prediction in past
            val predictionPast =
                objects.routePattern(route2) {
                    directionId = 1
                    sortOrder = 4
                    representativeTrip { headsign = "Prediction Past" }
                }
            // include, prediction in past but BRD
            val predictionBrd =
                objects.routePattern(route3) {
                    sortOrder = 5
                    representativeTrip { headsign = "Prediction BRD" }
                }
            // include, prediction upcoming
            val predictionSoon =
                objects.routePattern(route3) {
                    directionId = 1
                    sortOrder = 6
                    representativeTrip { headsign = "Prediction Soon" }
                }
            // exclude, prediction later
            val predictionLater =
                objects.routePattern(route4) {
                    sortOrder = 7
                    representativeTrip { headsign = "Prediction Later" }
                }
            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            stop1.id to
                                listOf(
                                    schedulePast.id,
                                    scheduleSoon.id,
                                    scheduleLater.id,
                                    predictionPast.id,
                                    predictionBrd.id,
                                    predictionSoon.id,
                                    predictionLater.id,
                                )
                        ),
                )

            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.SEPTEMBER, 19, 13, 43, 19)

            val schedulePastSchedule =
                objects.schedule {
                    stopId = stop1.id
                    trip = objects.trip(schedulePast)
                    departureTime = time - 1.minutes
                }
            val scheduleSoonSchedule =
                objects.schedule {
                    stopId = stop1.id
                    trip = objects.trip(scheduleSoon)
                    departureTime = time + 5.minutes
                }
            val scheduleLaterSchedule =
                objects.schedule {
                    stopId = stop1.id
                    trip = objects.trip(scheduleLater)
                    departureTime = time + 121.minutes
                }
            val predictionPastPrediction =
                objects.prediction {
                    stopId = stop1.id
                    trip = objects.trip(predictionPast)
                    departureTime = time - 1.minutes
                }
            val predictionBrdTrip = objects.trip(predictionBrd)
            val predictionBrdVehicle =
                objects.vehicle {
                    stopId = stop1.id
                    tripId = predictionBrdTrip.id
                    currentStatus = Vehicle.CurrentStatus.StoppedAt
                }
            val predictionBrdPrediction =
                objects.prediction {
                    stopId = stop1.id
                    trip = predictionBrdTrip
                    departureTime = time - 1.minutes
                    vehicleId = predictionBrdVehicle.id
                }
            val predictionSoonPrediction =
                objects.prediction {
                    stopId = stop1.id
                    trip = objects.trip(predictionSoon)
                    departureTime = time + 5.minutes
                }
            val predictionLaterPrediction =
                objects.prediction {
                    stopId = stop1.id
                    trip = objects.trip(predictionLater)
                    departureTime = time + 121.minutes
                }

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            val lineOrRoute3 = RouteCardData.LineOrRoute.Route(route3)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 1,
                                            routePatterns = listOf(scheduleSoon),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(objects.upcomingTrip(scheduleSoonSchedule)),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    ),
                    RouteCardData(
                        lineOrRoute = lineOrRoute3,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route3,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute3,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(predictionBrd),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        prediction = predictionBrdPrediction,
                                                        vehicle = predictionBrdVehicle,
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute3,
                                            stop = stop1,
                                            directionId = 1,
                                            routePatterns = listOf(predictionSoon),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(predictionSoonPrediction)
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                    ),
                                    global,
                                )
                            ),
                        time,
                    ),
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(stop1.id),
                    globalData = global,
                    sortByDistanceFrom = stop1.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList hides rare patterns while loading`() = runBlocking {
        val objects = ObjectCollectionBuilder()

        val stop1 = objects.stop()

        val route1 = objects.route()

        // should be included because typical and has prediction
        val typicalOutbound =
            objects.routePattern(route1) {
                directionId = 0
                sortOrder = 1
                typicality = RoutePattern.Typicality.Typical
                representativeTrip { headsign = "Typical Out" }
            }

        // should be excluded because no prediction
        val atypicalInbound =
            objects.routePattern(route1) {
                directionId = 1
                sortOrder = 6
                typicality = RoutePattern.Typicality.Atypical
                representativeTrip { headsign = "Atypical In" }
            }

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop = mapOf(stop1.id to listOf(typicalOutbound.id, atypicalInbound.id)),
            )

        val context = RouteCardData.Context.NearbyTransit
        val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

        val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
        assertEquals(
            listOf(
                RouteCardData(
                    lineOrRoute = lineOrRoute1,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                route1,
                                stop1,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = lineOrRoute1,
                                        stop = stop1,
                                        directionId = 0,
                                        routePatterns = listOf(typicalOutbound),
                                        stopIds = setOf(stop1.id),
                                        upcomingTrips = listOf(),
                                        alertsHere = emptyList(),
                                        allDataLoaded = false,
                                        hasSchedulesToday = false,
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    )
                                ),
                                global,
                            )
                        ),
                    time,
                )
            ),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop1.id),
                globalData = global,
                sortByDistanceFrom = stop1.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                now = time,
                pinnedRoutes = setOf(),
                context = context,
            ),
        )
    }

    @Test
    fun `RouteCardData routCardsForStopList doesn't filter future trips in filtered stop details`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val stop1 = objects.stop()

            val route1 = objects.route()

            // should be included because typical and has prediction
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }

            // should be included because all trips should be visible in filtered stop details
            val deviationInbound =
                objects.routePattern(route1) {
                    directionId = 1
                    sortOrder = 4
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip { headsign = "Deviation In" }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(stop1.id to listOf(typicalOutbound.id, deviationInbound.id)),
                )
            val context = RouteCardData.Context.StopDetailsFiltered
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val typicalOutboundPrediction =
                objects.prediction {
                    departureTime = time
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = typicalOutbound.representativeTripId
                }

            val deviationInboundPrediction =
                objects.prediction {
                    departureTime = time + 400.minutes
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = deviationInbound.representativeTripId
                }

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(typicalOutboundPrediction)
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 1,
                                            routePatterns = listOf(deviationInbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(deviationInboundPrediction)
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = false,
                                            hasSchedulesToday = false,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(stop1.id),
                    globalData = global,
                    sortByDistanceFrom = stop1.position,
                    schedules = null,
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList includes cancellations in filtered stop details context`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop1 = objects.stop()
            val route1 = objects.route { type = RouteType.BUS }

            // should be included because typical and has cancelled prediction
            val typicalOutbound =
                objects.routePattern(route1) {
                    directionId = 0
                    sortOrder = 1
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip { headsign = "Typical Out" }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop = mapOf(stop1.id to listOf(typicalOutbound.id)),
                )
            val context = RouteCardData.Context.StopDetailsFiltered
            val time = EasternTimeInstant(2024, Month.FEBRUARY, 22, 12, 8, 19)

            val typicalOutboundSchedule =
                objects.schedule {
                    routeId = route1.id
                    tripId = typicalOutbound.representativeTripId
                    stopId = stop1.id
                    arrivalTime = time
                    departureTime = time
                }

            val typicalOutboundPrediction =
                objects.prediction {
                    departureTime = null
                    routeId = route1.id
                    stopId = stop1.id
                    tripId = typicalOutbound.representativeTripId
                    scheduleRelationship = Prediction.ScheduleRelationship.Cancelled
                }

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop1,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop1,
                                            directionId = 0,
                                            routePatterns = listOf(typicalOutbound),
                                            stopIds = setOf(stop1.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        prediction = typicalOutboundPrediction,
                                                        schedule = typicalOutboundSchedule,
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(stop1.id),
                    globalData = global,
                    sortByDistanceFrom = stop1.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList handles parent stops`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val parentStop = objects.stop { childStopIds = listOf("childStop") }
        val childStop =
            objects.stop {
                id = "childStop"
                parentStationId = parentStop.id
            }
        val route1 = objects.route()
        val pattern1 = objects.routePattern(route1) { representativeTrip { headsign = "Harvard" } }

        val global =
            GlobalResponse(objects, patternIdsByStop = mapOf(childStop.id to listOf(pattern1.id)))
        val context = RouteCardData.Context.NearbyTransit
        val time = EasternTimeInstant(2024, Month.FEBRUARY, 26, 10, 45, 38)

        val prediction1 =
            objects.prediction {
                departureTime = time
                routeId = route1.id
                stopId = childStop.id
                tripId = pattern1.representativeTripId
            }

        val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
        assertEquals(
            listOf(
                RouteCardData(
                    lineOrRoute = lineOrRoute1,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                route1,
                                parentStop,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = lineOrRoute1,
                                        stop = parentStop,
                                        directionId = 0,
                                        routePatterns = listOf(pattern1),
                                        stopIds = setOf(parentStop.id, childStop.id),
                                        upcomingTrips = listOf(objects.upcomingTrip(prediction1)),
                                        alertsHere = emptyList(),
                                        allDataLoaded = false,
                                        hasSchedulesToday = false,
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    )
                                ),
                                global,
                            )
                        ),
                    time,
                )
            ),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(childStop.id, parentStop.id),
                globalData = global,
                sortByDistanceFrom = parentStop.position,
                schedules = null,
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                now = time,
                pinnedRoutes = setOf(),
                context = context,
            ),
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList incorporates schedules`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val stop = objects.stop()
        val route = objects.route()
        val routePattern = objects.routePattern(route) { representativeTrip { headsign = "A" } }
        val trip1 = objects.trip(routePattern)
        val trip2 = objects.trip(routePattern)

        val global =
            GlobalResponse(objects, patternIdsByStop = mapOf(stop.id to listOf(routePattern.id)))
        val context = RouteCardData.Context.NearbyTransit
        val time = EasternTimeInstant(2024, Month.MARCH, 14, 12, 23, 44)

        val sched1 =
            objects.schedule {
                trip = trip1
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val sched2 =
            objects.schedule {
                trip = trip2
                stopId = stop.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }

        val pred1 = objects.prediction(sched1) { departureTime = time + 1.5.minutes }
        val pred2 = objects.prediction(sched2) { departureTime = null }

        val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
        assertEquals(
            listOf(
                RouteCardData(
                    lineOrRoute = lineOrRoute,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                route,
                                stop,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = lineOrRoute,
                                        stop = stop,
                                        directionId = 0,
                                        routePatterns = listOf(routePattern),
                                        stopIds = setOf(stop.id),
                                        upcomingTrips =
                                            listOf(
                                                objects.upcomingTrip(
                                                    prediction = pred1,
                                                    schedule = sched1,
                                                ),
                                                objects.upcomingTrip(
                                                    prediction = pred2,
                                                    schedule = sched2,
                                                ),
                                            ),
                                        alertsHere = emptyList(),
                                        allDataLoaded = true,
                                        hasSchedulesToday = true,
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    )
                                ),
                                global,
                            )
                        ),
                    time,
                )
            ),
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(stop.id),
                globalData = global,
                sortByDistanceFrom = stop.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(objects),
                now = time,
                pinnedRoutes = setOf(),
                context = context,
            ),
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList checks if any trips are scheduled all day`(): Unit =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route = objects.route()
            val routePatternA =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip {
                        headsign = "A"
                        stopIds = listOf(stop.id)
                    }
                }
            val routePatternB =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    representativeTrip {
                        headsign = "B"
                        stopIds = listOf(stop.id)
                    }
                }
            val routePatternC =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Deviation
                    representativeTrip {
                        headsign = "C"
                        stopIds = listOf(stop.id)
                    }
                }
            val trip1 = objects.trip(routePatternA)

            val time = EasternTimeInstant(2024, Month.MARCH, 14, 12, 23, 44)

            objects.schedule {
                trip = trip1
                stopId = stop.id
                stopSequence = 90
                departureTime = time - 2.hours
            }

            val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
            val context = RouteCardData.Context.NearbyTransit
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute,
                        listOf(
                            RouteCardData.RouteStopData(
                                route,
                                stop,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute,
                                        stop,
                                        directionId = 0,
                                        listOf(routePatternA, routePatternB, routePatternC),
                                        setOf(stop.id),
                                        upcomingTrips = emptyList(),
                                        alertsHere = emptyList(),
                                        allDataLoaded = true,
                                        hasSchedulesTodayByPattern =
                                            mapOf(
                                                routePatternA.id to true,
                                                routePatternB.id to false,
                                                routePatternC.id to false,
                                            ),
                                        alertsDownstream = emptyList(),
                                        context,
                                    )
                                ),
                                globalData = GlobalResponse(objects),
                            )
                        ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(stop.id),
                    globalData = GlobalResponse(objects),
                    sortByDistanceFrom = stop.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    pinnedRoutes = setOf(),
                    now = time,
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList checks route along with route pattern and stop`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route1 = objects.route { sortOrder = 1 }
            val routePattern1 =
                objects.routePattern(route1) { representativeTrip { headsign = "A" } }
            val trip1 = objects.trip(routePattern1)

            val route2 = objects.route { sortOrder = 2 }
            val routePattern2 =
                objects.routePattern(route2) { representativeTrip { headsign = "B" } }
            val trip2 = objects.trip(routePattern2)

            // change from before: this will be included because it has a matching
            // route & direction, even if it is an unexpected route pattern.
            val trip3 = objects.trip(routePattern2) { routePatternId = "not the right id" }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop = mapOf(stop.id to listOf(routePattern1.id, routePattern2.id)),
                )
            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.MARCH, 18, 10, 41, 13)

            val sched1 =
                objects.schedule {
                    trip = trip1
                    stopId = stop.id
                    stopSequence = 90
                    departureTime = time + 1.minutes
                }
            val sched2 =
                objects.schedule {
                    trip = trip2
                    stopId = stop.id
                    stopSequence = 90
                    departureTime = time + 2.minutes
                }
            val sched3 =
                objects.schedule {
                    trip = trip3
                    stopId = stop.id
                    stopSequence = 90
                    departureTime = time + 3.minutes
                }

            val pred1 = objects.prediction(sched1) { departureTime = time + 1.5.minutes }
            val pred2 = objects.prediction(sched2) { departureTime = time + 2.3.minutes }
            val pred3 = objects.prediction(sched3) { departureTime = time + 3.4.minutes }

            val lineOrRoute1 = RouteCardData.LineOrRoute.Route(route1)
            val lineOrRoute2 = RouteCardData.LineOrRoute.Route(route2)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute1,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route1,
                                    stop,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute1,
                                            stop = stop,
                                            directionId = 0,
                                            routePatterns = listOf(routePattern1),
                                            stopIds = setOf(stop.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        prediction = pred1,
                                                        schedule = sched1,
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    ),
                    RouteCardData(
                        lineOrRoute = lineOrRoute2,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route2,
                                    stop,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute2,
                                            stop = stop,
                                            directionId = 0,
                                            routePatterns = listOf(routePattern2),
                                            stopIds = setOf(stop.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        prediction = pred2,
                                                        schedule = sched2,
                                                    ),
                                                    objects.upcomingTrip(
                                                        prediction = pred3,
                                                        schedule = sched3,
                                                    ),
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    ),
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(stop.id),
                    globalData = global,
                    sortByDistanceFrom = stop.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList groups lines by direction`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val hynes = objects.stop { id = "place-hymnl" }

        val gov = objects.stop { id = "place-gover" }

        // These stops are included because they're thresholds in Direction.specialCases
        val boylston = objects.stop { id = "place-boyls" }
        val arlington = objects.stop { id = "place-armnl" }
        val haymarket = objects.stop { id = "place-haecl" }

        val bc = objects.stop { id = "place-lake" }
        val cleCenter = objects.stop { id = "place-clmnl" }
        val heath = objects.stop { id = "place-hsmnl" }
        val magoun = objects.stop { id = "place-mgngl" }

        val line = objects.line { id = "line-Green" }
        val routeB =
            objects.route {
                id = "Green-B"
                sortOrder = 1
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Ignored West B", "Ignored East B")
            }
        val routePatternB1 =
            objects.routePattern(routeB) {
                representativeTrip {
                    headsign = "B"
                    stopIds = listOf(gov.id, boylston.id, arlington.id, hynes.id, bc.id)
                }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternB2 =
            objects.routePattern(routeB) {
                representativeTrip {
                    headsign = "B"
                    stopIds = listOf(bc.id, hynes.id, arlington.id, boylston.id, gov.id)
                }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripB1 = objects.trip(routePatternB1)
        val tripB2 = objects.trip(routePatternB2)

        val routeC =
            objects.route {
                id = "Green-C"
                sortOrder = 2
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Ignored West C", "Ignored East C")
            }
        val routePatternC1 =
            objects.routePattern(routeC) {
                representativeTrip {
                    headsign = "C"
                    stopIds = listOf(gov.id, boylston.id, arlington.id, hynes.id, cleCenter.id)
                }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternC2 =
            objects.routePattern(routeC) {
                representativeTrip {
                    headsign = "C"
                    stopIds = listOf(cleCenter.id, hynes.id, arlington.id, boylston.id, gov.id)
                }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripC1 = objects.trip(routePatternC1)
        val tripC2 = objects.trip(routePatternC2)

        val routeE =
            objects.route {
                id = "Green-E"
                sortOrder = 3
                lineId = "line-Green"
                directionNames = listOf("West", "East")
                directionDestinations = listOf("Ignored West E", "Ignored East E")
            }
        val routePatternE1 =
            objects.routePattern(routeE) {
                id = "test-hs"
                representativeTrip {
                    headsign = "Heath Street"
                    stopIds =
                        listOf(
                            magoun.id,
                            haymarket.id,
                            gov.id,
                            boylston.id,
                            arlington.id,
                            hynes.id,
                            heath.id,
                        )
                }
                directionId = 0
                typicality = RoutePattern.Typicality.Typical
            }
        val routePatternE2 =
            objects.routePattern(routeE) {
                representativeTrip {
                    headsign = "Medford/Tufts"
                    stopIds =
                        listOf(
                            heath.id,
                            hynes.id,
                            arlington.id,
                            boylston.id,
                            gov.id,
                            haymarket.id,
                            magoun.id,
                        )
                }
                directionId = 1
                typicality = RoutePattern.Typicality.Typical
            }
        val tripE1 = objects.trip(routePatternE1)
        val tripE2 = objects.trip(routePatternE2)

        val time = EasternTimeInstant(2024, Month.MARCH, 18, 10, 41, 13)

        val schedB1 =
            objects.schedule {
                trip = tripB1
                stopId = hynes.id
                stopSequence = 90
                departureTime = time + 1.minutes
            }
        val schedB2 =
            objects.schedule {
                trip = tripB2
                stopId = hynes.id
                stopSequence = 90
                departureTime = time + 4.minutes
            }
        val schedC1 =
            objects.schedule {
                trip = tripC1
                stopId = hynes.id
                stopSequence = 90
                departureTime = time + 2.minutes
            }
        val schedC2 =
            objects.schedule {
                trip = tripC2
                stopId = hynes.id
                stopSequence = 90
                departureTime = time + 5.minutes
            }
        val schedE1 =
            objects.schedule {
                trip = tripE1
                stopId = hynes.id
                stopSequence = 90
                departureTime = time + 3.minutes
            }
        val schedE2 =
            objects.schedule {
                trip = tripE2
                stopId = hynes.id
                stopSequence = 90
                departureTime = time + 6.minutes
            }

        val predB1 = objects.prediction(schedB1) { departureTime = time + 1.5.minutes }
        val predB2 = objects.prediction(schedB2) { departureTime = time + 4.5.minutes }
        val predC1 = objects.prediction(schedC1) { departureTime = time + 2.3.minutes }
        val predC2 = objects.prediction(schedC2) { departureTime = time + 5.3.minutes }
        val predE1 = objects.prediction(schedE1) { departureTime = time + 2.3.minutes }
        val predE2 = objects.prediction(schedE2) { departureTime = time + 6.3.minutes }

        val directionWest = Direction("West", "Kenmore & West", 0)
        val directionEast = Direction("East", "Park St & North", 1)

        val global =
            GlobalResponse(
                objects,
                patternIdsByStop =
                    mapOf(
                        hynes.id to
                            listOf(
                                routePatternB1.id,
                                routePatternB2.id,
                                routePatternC1.id,
                                routePatternC2.id,
                                routePatternE1.id,
                                routePatternE2.id,
                            )
                    ),
            )
        val context = RouteCardData.Context.NearbyTransit

        val lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(routeB, routeC, routeE))
        val expected =
            listOf(
                RouteCardData(
                    lineOrRoute = lineOrRoute,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                lineOrRoute,
                                hynes,
                                listOf(directionWest, directionEast),
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = lineOrRoute,
                                        stop = hynes,
                                        directionId = 0,
                                        routePatterns =
                                            listOf(routePatternB1, routePatternC1, routePatternE1),
                                        stopIds = setOf(hynes.id),
                                        upcomingTrips =
                                            listOf(
                                                objects.upcomingTrip(
                                                    prediction = predB1,
                                                    schedule = schedB1,
                                                ),
                                                objects.upcomingTrip(
                                                    prediction = predC1,
                                                    schedule = schedC1,
                                                ),
                                                objects.upcomingTrip(
                                                    prediction = predE1,
                                                    schedule = schedE1,
                                                ),
                                            ),
                                        alertsHere = emptyList(),
                                        allDataLoaded = true,
                                        hasSchedulesTodayByPattern =
                                            mapOf(
                                                routePatternB1.id to true,
                                                routePatternC1.id to true,
                                                routePatternE1.id to true,
                                            ),
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    ),
                                    RouteCardData.Leaf(
                                        lineOrRoute = lineOrRoute,
                                        stop = hynes,
                                        directionId = 1,
                                        routePatterns =
                                            listOf(routePatternB2, routePatternC2, routePatternE2),
                                        stopIds = setOf(hynes.id),
                                        upcomingTrips =
                                            listOf(
                                                objects.upcomingTrip(
                                                    prediction = predB2,
                                                    schedule = schedB2,
                                                ),
                                                objects.upcomingTrip(
                                                    prediction = predC2,
                                                    schedule = schedC2,
                                                ),
                                                objects.upcomingTrip(
                                                    prediction = predE2,
                                                    schedule = schedE2,
                                                ),
                                            ),
                                        alertsHere = emptyList(),
                                        allDataLoaded = true,
                                        hasSchedulesTodayByPattern =
                                            mapOf(
                                                routePatternB2.id to true,
                                                routePatternC2.id to true,
                                                routePatternE2.id to true,
                                            ),
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    ),
                                ),
                            )
                        ),
                    time,
                )
            )

        assertEquals(
            expected,
            RouteCardData.routeCardsForStopList(
                stopIds = listOf(hynes.id),
                globalData = global,
                sortByDistanceFrom = hynes.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(emptyMap()),
                now = time,
                pinnedRoutes = setOf(),
                context = context,
            ),
        )
    }

    @Test
    fun `RouteCardData routeCardsForStopList for line on branch uses branch direction name`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val bc = objects.stop { id = "place-lake" }

            val southSt = objects.stop { id = "place-sougr" }

            val arlington = objects.stop { id = "place-armnl" }
            val boylston = objects.stop { id = "place-boyls" }
            val gov = objects.stop { id = "place-gover" }
            val haymarket = objects.stop { id = "place-haecl" }

            val line = objects.line { id = "line-Green" }
            val routeB =
                objects.route {
                    id = "Green-B"
                    sortOrder = 1
                    lineId = "line-Green"
                    directionNames = listOf("West", "East")
                    directionDestinations = listOf("Real B West", "Real B East")
                }
            val routePatternB1 =
                objects.routePattern(routeB) {
                    representativeTrip {
                        headsign = "B"
                        stopIds = listOf(gov.id, boylston.id, arlington.id, southSt.id, bc.id)
                    }
                    directionId = 0
                    typicality = RoutePattern.Typicality.Typical
                }
            val routePatternB2 =
                objects.routePattern(routeB) {
                    representativeTrip {
                        headsign = "B"
                        stopIds = listOf(bc.id, southSt.id, arlington.id, boylston.id, gov.id)
                    }
                    directionId = 1
                    typicality = RoutePattern.Typicality.Typical
                }
            val tripB1 = objects.trip(routePatternB1)
            val tripB2 = objects.trip(routePatternB2)

            val time = EasternTimeInstant(2024, Month.MARCH, 18, 10, 41, 13)

            val schedB1 =
                objects.schedule {
                    trip = tripB1
                    stopId = southSt.id
                    stopSequence = 90
                    departureTime = time + 1.minutes
                }
            val schedB2 =
                objects.schedule {
                    trip = tripB2
                    stopId = southSt.id
                    stopSequence = 90
                    departureTime = time + 4.minutes
                }

            val predB1 = objects.prediction(schedB1) { departureTime = time + 1.5.minutes }
            val predB2 = objects.prediction(schedB2) { departureTime = time + 4.5.minutes }
            val directionWest = Direction("West", routeB.directionDestinations[0], 0)
            val directionEast = Direction("East", "Park St & North", 1)

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(southSt.id to listOf(routePatternB1.id, routePatternB2.id)),
                )
            val context = RouteCardData.Context.NearbyTransit

            val lineOrRoute = RouteCardData.LineOrRoute.Line(line, setOf(routeB))
            val expected =
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    lineOrRoute,
                                    southSt,
                                    listOf(directionWest, directionEast),
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute,
                                            stop = southSt,
                                            directionId = 0,
                                            routePatterns = listOf(routePatternB1),
                                            stopIds = setOf(southSt.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        prediction = predB1,
                                                        schedule = schedB1,
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute,
                                            stop = southSt,
                                            directionId = 1,
                                            routePatterns = listOf(routePatternB2),
                                            stopIds = setOf(southSt.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        prediction = predB2,
                                                        schedule = schedB2,
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        ),
                                    ),
                                )
                            ),
                        time,
                    )
                )

            assertEquals(
                expected,
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(southSt.id),
                    globalData = global,
                    sortByDistanceFrom = southSt.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList direction names for regular route pulled from route data`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()
            val stop = objects.stop()
            val route1 =
                objects.route {
                    sortOrder = 1
                    directionNames = listOf("Direction 0", "Direction 1")
                    directionDestinations =
                        listOf("Direction 0 destination", "Direction 1 destination")
                }

            val routePattern1 =
                objects.routePattern(route1) { representativeTrip { headsign = "A" } }
            val trip1 = objects.trip(routePattern1)

            val time = EasternTimeInstant(2024, Month.MARCH, 18, 10, 41, 13)

            val sched1 =
                objects.schedule {
                    trip = trip1
                    stopId = stop.id
                    stopSequence = 90
                    departureTime = time + 1.minutes
                }

            val pred1 = objects.prediction(sched1) { departureTime = time + 1.5.minutes }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop = mapOf(stop.id to listOf(routePattern1.id)),
                )
            val context = RouteCardData.Context.NearbyTransit

            val lineOrRoute = RouteCardData.LineOrRoute.Route(route1)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    lineOrRoute,
                                    stop,
                                    listOf(
                                        Direction(
                                            id = 0,
                                            name = "Direction 0",
                                            destination = "Direction 0 destination",
                                        ),
                                        Direction(
                                            id = 1,
                                            name = "Direction 1",
                                            destination = "Direction 1 destination",
                                        ),
                                    ),
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute,
                                            stop = stop,
                                            directionId = 0,
                                            routePatterns = listOf(routePattern1),
                                            stopIds = setOf(stop.id),
                                            upcomingTrips =
                                                listOf(
                                                    objects.upcomingTrip(
                                                        prediction = pred1,
                                                        schedule = sched1,
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(stop.id),
                    globalData = global,
                    sortByDistanceFrom = stop.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList north station disruption case`() = runBlocking {
        val objects = ObjectCollectionBuilder()
        val northStation =
            objects.stop {
                id = "place-north"
                locationType = LocationType.STATION
                childStopIds = listOf("70027", "70026")
            }

        val northStationNorthboundPlatform =
            objects.stop {
                id = "70027"
                locationType = LocationType.STOP
                parentStationId = "place-north"
            }

        val northStationSouthboundPlatform =
            objects.stop {
                id = "70026"
                locationType = LocationType.STOP
                parentStationId = "place-north"
            }

        val oakGrove =
            objects.stop {
                id = "place-ogmnl"
                locationType = LocationType.STATION
                childStopIds = listOf("70036")
            }

        val oakGrovePlatform =
            objects.stop {
                id = "70036"
                locationType = LocationType.STOP
                parentStationId = "place-ogmnl"
            }

        val forestHills =
            objects.stop {
                id = "place-forhl"
                locationType = LocationType.STATION
                childStopIds = listOf("70001")
            }

        val forestHillsPlatform =
            objects.stop {
                id = "70001"
                locationType = LocationType.STOP
                parentStationId = "place-forhl"
            }

        val orangeRoute = objects.route { id = "Orange" }
        val orangeNorthboundTypical =
            objects.routePattern(orangeRoute) {
                id = "Orange-3-1"
                typicality = RoutePattern.Typicality.Typical
                directionId = 1
                representativeTrip {
                    id = "canonical-Orange-C1-1"
                    headsign = "Oak Grove"
                    directionId = 1
                    stopIds = listOf("70001", "70027", "70036")
                }
            }

        val orangeNorthboundDiversion =
            objects.routePattern(orangeRoute) {
                id = "Orange-6-1"
                typicality = RoutePattern.Typicality.Diversion
                directionId = 1
                representativeTrip {
                    id = "65686489"
                    headsign = "North Station"
                    directionId = 1
                    stopIds = listOf("70001", "70027")
                }
            }

        val orangeSouthboundTypical =
            objects.routePattern(orangeRoute) {
                id = "Orange-3-0"
                typicality = RoutePattern.Typicality.Typical
                directionId = 0
                representativeTrip {
                    id = "canonical-Orange-C1-0"
                    headsign = "Forest Hills"
                    directionId = 0
                    stopIds = listOf("70036", "70026", "70001")
                }
            }

        val orangeSouthboundDiversion =
            objects.routePattern(orangeRoute) {
                id = "Orange-6-0"
                typicality = RoutePattern.Typicality.Diversion
                directionId = 0
                representativeTrip {
                    id = "65743311"
                    headsign = "Forest Hills"
                    directionId = 0
                    stopIds = listOf("70026", "70001")
                }
            }

        val orangeNorthboundTypicalTrip =
            objects.trip(orangeNorthboundTypical) { id = "northboundTypical" }

        val orangeNorthboundDiversionTrip =
            objects.trip(orangeNorthboundDiversion) { id = "northboundDiversion" }

        val orangeSouthboundDiversionTrip = objects.trip(orangeSouthboundDiversion)

        val time = EasternTimeInstant(2024, Month.OCTOBER, 30, 16, 40)

        // Scheduled northbound arrivals with North station as last stop
        val northboundSchedule =
            objects.schedule {
                trip = orangeNorthboundDiversionTrip
                stopId = northStationNorthboundPlatform.id
                stopSequence = 130
                arrivalTime = time + 4.minutes
                departureTime = null
            }

        val northboundPrediction =
            objects.prediction {
                trip = orangeNorthboundTypicalTrip
                stopId = northStationNorthboundPlatform.id
                stopSequence = 130
                arrivalTime = time + 8.minutes
                departureTime = null
            }

        // Scheduled southbound departures
        val southboundSchedule =
            objects.schedule {
                trip = orangeSouthboundDiversionTrip
                stopId = northStationSouthboundPlatform.id
                stopSequence = 60
                arrivalTime = null
                departureTime = time + 6.minutes
            }

        val alert =
            objects.alert {
                id = "601685"
                activePeriod(
                    EasternTimeInstant(2024, Month.OCTOBER, 28, 3, 0),
                    EasternTimeInstant(2024, Month.NOVEMBER, 2, 3, 0),
                )
                cause = Alert.Cause.Maintenance
                effect = Alert.Effect.Shuttle
                informedEntity =
                    mutableListOf(
                        // North station entities
                        Alert.InformedEntity(
                            activities =
                                listOf(
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = northStationSouthboundPlatform.id,
                        ),
                        Alert.InformedEntity(
                            activities =
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = northStationNorthboundPlatform.id,
                        ),
                        Alert.InformedEntity(
                            activities =
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = northStation.id,
                        ),
                        Alert.InformedEntity(
                            activities =
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = oakGrove.id,
                        ),
                        Alert.InformedEntity(
                            activities =
                                listOf(
                                    Alert.InformedEntity.Activity.Board,
                                    Alert.InformedEntity.Activity.Exit,
                                    Alert.InformedEntity.Activity.Ride,
                                ),
                            route = orangeRoute.id,
                            routeType = RouteType.HEAVY_RAIL,
                            stop = oakGrovePlatform.id,
                        ),
                    )
            }

        val global = GlobalResponse(objects)
        val context = RouteCardData.Context.NearbyTransit

        val orangeLineOrRoute = RouteCardData.LineOrRoute.Route(orangeRoute)
        assertEquals(
            listOf(
                RouteCardData(
                    lineOrRoute = orangeLineOrRoute,
                    stopData =
                        listOf(
                            RouteCardData.RouteStopData(
                                orangeRoute,
                                northStation,
                                listOf(
                                    RouteCardData.Leaf(
                                        lineOrRoute = orangeLineOrRoute,
                                        stop = northStation,
                                        directionId = 0,
                                        routePatterns =
                                            listOf(
                                                orangeSouthboundTypical,
                                                orangeSouthboundDiversion,
                                            ),
                                        stopIds = setOf(northStationSouthboundPlatform.id),
                                        upcomingTrips =
                                            listOf(
                                                UpcomingTrip(
                                                    trip = orangeSouthboundDiversionTrip,
                                                    schedule = southboundSchedule,
                                                )
                                            ),
                                        alertsHere = emptyList(),
                                        allDataLoaded = true,
                                        hasSchedulesTodayByPattern =
                                            mapOf(
                                                orangeSouthboundDiversion.id to true,
                                                orangeSouthboundTypical.id to false,
                                            ),
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    ),
                                    RouteCardData.Leaf(
                                        lineOrRoute = orangeLineOrRoute,
                                        stop = northStation,
                                        directionId = 1,
                                        routePatterns =
                                            listOf(
                                                orangeNorthboundTypical,
                                                orangeNorthboundDiversion,
                                            ),
                                        stopIds = setOf(northStationNorthboundPlatform.id),
                                        upcomingTrips = listOf(),
                                        alertsHere = listOf(alert),
                                        allDataLoaded = true,
                                        hasSchedulesTodayByPattern =
                                            mapOf(
                                                orangeNorthboundDiversion.id to true,
                                                orangeNorthboundTypical.id to false,
                                            ),
                                        alertsDownstream = emptyList(),
                                        context = context,
                                    ),
                                ),
                                global,
                            )
                        ),
                    time,
                )
            ),
            RouteCardData.routeCardsForStopList(
                listOf(
                    northStation.id,
                    northStationSouthboundPlatform.id,
                    northStationNorthboundPlatform.id,
                ),
                globalData = global,
                sortByDistanceFrom = northStation.position,
                schedules = ScheduleResponse(objects),
                predictions = PredictionsStreamDataResponse(objects),
                alerts = AlertsStreamDataResponse(mapOf(alert.id to alert)),
                now = time,
                pinnedRoutes = setOf(),
                context = context,
            ),
        )
    }

    @Test
    fun `RouteCardData routeCardsForList filters out direction if it is the typical last stop of subway`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val oakGrove =
                objects.stop {
                    id = "place-ogmnl"
                    locationType = LocationType.STATION
                    childStopIds = listOf("70036")
                }

            val orangeRoute = objects.route { id = "Orange" }
            val orangeNorthboundTypical =
                objects.routePattern(orangeRoute) {
                    id = "Orange-3-1"
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 1
                    representativeTrip {
                        id = "canonical-Orange-C1-1"
                        headsign = "Oak Grove"
                        directionId = 1
                        stopIds = listOf("70001", "70027", "70036")
                    }
                }

            val orangeSouthboundTypical =
                objects.routePattern(orangeRoute) {
                    id = "Orange-3-0"
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 0
                    representativeTrip {
                        id = "canonical-Orange-C1-0"
                        headsign = "Forest Hills"
                        directionId = 0
                        stopIds = listOf("70036", "70026", "70001")
                    }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            oakGrove.id to
                                listOf(orangeSouthboundTypical.id, orangeNorthboundTypical.id)
                        ),
                )
            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.OCTOBER, 30, 16, 40)

            val orangeNorthboundTypicalTrip = objects.trip(orangeNorthboundTypical)
            val orangeSouthboundTypicalTrip = objects.trip(orangeSouthboundTypical)

            val sched1 =
                objects.schedule {
                    trip = orangeSouthboundTypicalTrip
                    stopId = oakGrove.id
                    stopSequence = 90
                    departureTime = time + 1.minutes
                }
            val sched2 =
                objects.schedule {
                    trip = orangeNorthboundTypicalTrip
                    stopId = oakGrove.id
                    stopSequence = 90
                    arrivalTime = time + 2.minutes
                    departureTime = null
                }

            val lineOrRoute = RouteCardData.LineOrRoute.Route(orangeRoute)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    orangeRoute,
                                    oakGrove,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute,
                                            stop = oakGrove,
                                            directionId = 0,
                                            routePatterns = listOf(orangeSouthboundTypical),
                                            stopIds = setOf(oakGrove.id),
                                            upcomingTrips =
                                                listOf(
                                                    UpcomingTrip(
                                                        trip = orangeSouthboundTypicalTrip,
                                                        schedule = sched1,
                                                    )
                                                ),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(oakGrove.id),
                    globalData = global,
                    sortByDistanceFrom = oakGrove.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList filters out any arrival only for non-subway routes`() =
        runBlocking {
            val objects = ObjectCollectionBuilder()

            val longWharf = objects.stop { id = "Boat-Long" }

            val ferryRoute =
                objects.route {
                    id = "Boat-F1"
                    type = RouteType.FERRY
                }
            val ferryInboundToLongWharf =
                objects.routePattern(ferryRoute) {
                    id = "Boat-F1-3-1"
                    routeId = ferryRoute.id
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 1
                    representativeTrip {
                        id = "Boat-F1-0730-Hull-BF2H-01-Weekday-Fall-24"
                        headsign = "Long Wharf"
                        directionId = 1
                        stopIds = listOf("Boat-Hull", "Boat-Long")
                    }
                }

            val ferryOutboundToHingham =
                objects.routePattern(ferryRoute) {
                    id = "Boat-F1-0-0"
                    routeId = ferryRoute.id
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 0
                    representativeTrip {
                        id = "Boat-F1-1100-Long-BF2H-01-Weekday-Fall-24"
                        headsign = "Hingham"
                        directionId = 0
                        stopIds = listOf("Boat-Long", "Boat-Logan", "Boat-Hull", "Boat-Hingham")
                    }
                }

            val global =
                GlobalResponse(
                    objects,
                    patternIdsByStop =
                        mapOf(
                            longWharf.id to
                                listOf(ferryInboundToLongWharf.id, ferryOutboundToHingham.id)
                        ),
                )
            val context = RouteCardData.Context.NearbyTransit
            val time = EasternTimeInstant(2024, Month.OCTOBER, 30, 16, 40)

            val ferryInboundTrip = objects.trip(ferryInboundToLongWharf)
            val ferryOutboundTrip = objects.trip(ferryOutboundToHingham)

            val schedInbound =
                objects.schedule {
                    trip = ferryInboundTrip
                    stopId = longWharf.id
                    stopSequence = 90
                    arrivalTime = time + 2.minutes
                    departureTime = null
                }
            val schedOutbound =
                objects.schedule {
                    trip = ferryOutboundTrip
                    stopId = longWharf.id
                    stopSequence = 90
                    departureTime = time + 2.minutes
                }

            val ferryLineOrRoute = RouteCardData.LineOrRoute.Route(ferryRoute)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = ferryLineOrRoute,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    ferryRoute,
                                    longWharf,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = ferryLineOrRoute,
                                            stop = longWharf,
                                            directionId = 0,
                                            routePatterns = listOf(ferryOutboundToHingham),
                                            stopIds = setOf(longWharf.id),
                                            upcomingTrips =
                                                listOf(objects.upcomingTrip(schedOutbound)),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesToday = true,
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(longWharf.id),
                    globalData = global,
                    sortByDistanceFrom = longWharf.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(emptyMap()),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList returns alertsHere and downstream alerts`() =
        runBlocking() {
            val context = RouteCardData.Context.NearbyTransit
            val objects = ObjectCollectionBuilder()

            val route = objects.route()
            val park = objects.stop { id = "park" }
            val alewife = objects.stop { id = "alewife" }
            val shawmut = objects.stop { id = "shawmut" }
            val ashmont = objects.stop { id = "ashmont" }
            val quincyAdams = objects.stop { id = "quincy_adams" }
            val braintree = objects.stop { id = "braintree" }
            val routePatternAshmont =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 0
                    representativeTrip {
                        directionId = 0
                        headsign = "Ashmont"
                        stopIds = listOf(alewife.id, park.id, shawmut.id, ashmont.id)
                    }
                }
            val routePatternBraintree =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 0
                    representativeTrip {
                        directionId = 0
                        headsign = "Braintree"
                        stopIds = listOf(alewife.id, park.id, quincyAdams.id, braintree.id)
                    }
                }

            val time = EasternTimeInstant.now()

            val tripBraintree = objects.trip(routePatternBraintree)
            val scheduleBraintree =
                objects.schedule {
                    trip = tripBraintree
                    departureTime = time + 2.minutes
                }
            val upcomingTripBraintree = objects.upcomingTrip(scheduleBraintree)

            val tripAshmont = objects.trip(routePatternAshmont)
            val scheduleAshmont =
                objects.schedule {
                    trip = tripAshmont
                    departureTime = time + 10.minutes
                }
            val upcomingTripAshmont = objects.upcomingTrip(scheduleAshmont)

            val shawmutShuttleAlert =
                objects.alert {
                    effect = Alert.Effect.Shuttle
                    activePeriod(time - 1.seconds, null)
                    informedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                        route = route.id,
                        stop = shawmut.id,
                    )
                }

            val ashmontShuttleAlert =
                objects.alert {
                    effect = Alert.Effect.Shuttle
                    activePeriod(time - 1.seconds, null)
                    informedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                        route = route.id,
                        stop = ashmont.id,
                    )
                }

            val parkShuttleAlert =
                objects.alert {
                    id = "park_shuttle_alert"
                    effect = Alert.Effect.Shuttle
                    activePeriod(time - 1.seconds, null)
                    informedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                        route = route.id,
                        stop = park.id,
                    )
                }

            val parkElevatorAlert =
                objects.alert {
                    id = "park_elevator_alert"
                    effect = Alert.Effect.ElevatorClosure
                    activePeriod(time - 1.seconds, null)
                    informedEntity(
                        listOf(Alert.InformedEntity.Activity.UsingWheelchair),
                        route = route.id,
                        stop = park.id,
                    )
                }

            val global =
                GlobalResponse(
                    objects,
                    mapOf(Pair(park.id, listOf(routePatternAshmont.id, routePatternBraintree.id))),
                )
            val southboundDownstreamAlerts =
                Alert.alertsDownstreamForPatterns(
                    alerts =
                        listOf(
                            ashmontShuttleAlert,
                            shawmutShuttleAlert,
                            parkShuttleAlert,
                            parkElevatorAlert,
                        ),
                    patterns = listOf(routePatternAshmont, routePatternBraintree),
                    targetStopWithChildren = setOf(park.id),
                    tripsById = global.trips,
                )
            val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route,
                                    park,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute,
                                            stop = park,
                                            directionId = 0,
                                            routePatterns =
                                                listOf(routePatternAshmont, routePatternBraintree),
                                            stopIds = setOf(park.id),
                                            upcomingTrips = emptyList(),
                                            alertsHere =
                                                listOf(parkShuttleAlert, parkElevatorAlert),
                                            allDataLoaded = true,
                                            hasSchedulesTodayByPattern =
                                                mapOf(
                                                    routePatternAshmont.id to true,
                                                    routePatternBraintree.id to true,
                                                ),
                                            alertsDownstream = southboundDownstreamAlerts,
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(park.id),
                    globalData = global,
                    sortByDistanceFrom = park.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }

    @Test
    fun `RouteCardData routeCardsForStopList suppresses track change alerts at core CR stops`() =
        runBlocking() {
            val context = RouteCardData.Context.StopDetailsFiltered
            val objects = ObjectCollectionBuilder()

            val route = objects.route()
            val southStation = objects.stop { id = "place-sstat" }
            val providence = objects.stop { id = "place-NEC-1851" }

            val routePatternPvd =
                objects.routePattern(route) {
                    typicality = RoutePattern.Typicality.Typical
                    directionId = 0
                    representativeTrip {
                        directionId = 0
                        headsign = "Providence"
                        stopIds = listOf(southStation.id, providence.id)
                    }
                }

            val time = EasternTimeInstant.now()

            val tripPvd = objects.trip(routePatternPvd)
            val schedulePvd =
                objects.schedule {
                    trip = tripPvd
                    departureTime = time + 2.minutes
                }
            val upcomingTripPvd = objects.upcomingTrip(schedulePvd)

            val southStationTrackChangeAlert =
                objects.alert {
                    effect = Alert.Effect.TrackChange
                    activePeriod(time - 1.seconds, null)
                    informedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                        route = route.id,
                        stop = southStation.id,
                    )
                }

            val providenceTrackChangeAlert =
                objects.alert {
                    effect = Alert.Effect.TrackChange
                    activePeriod(time - 1.seconds, null)
                    informedEntity(
                        listOf(
                            Alert.InformedEntity.Activity.Board,
                            Alert.InformedEntity.Activity.Ride,
                        ),
                        route = route.id,
                        stop = providence.id,
                    )
                }

            val global =
                GlobalResponse(
                    objects,
                    mapOf(
                        Pair(southStation.id, listOf(routePatternPvd.id)),
                        Pair(providence.id, listOf(routePatternPvd.id)),
                    ),
                )

            val lineOrRoute = RouteCardData.LineOrRoute.Route(route)
            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route,
                                    southStation,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute,
                                            stop = southStation,
                                            directionId = 0,
                                            routePatterns = listOf(routePatternPvd),
                                            stopIds = setOf(southStation.id),
                                            upcomingTrips = emptyList(),
                                            alertsHere = emptyList(),
                                            allDataLoaded = true,
                                            hasSchedulesTodayByPattern =
                                                mapOf(routePatternPvd.id to true),
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(southStation.id),
                    globalData = global,
                    sortByDistanceFrom = southStation.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )

            assertEquals(
                listOf(
                    RouteCardData(
                        lineOrRoute = lineOrRoute,
                        stopData =
                            listOf(
                                RouteCardData.RouteStopData(
                                    route,
                                    providence,
                                    listOf(
                                        RouteCardData.Leaf(
                                            lineOrRoute = lineOrRoute,
                                            stop = providence,
                                            directionId = 0,
                                            routePatterns = listOf(routePatternPvd),
                                            stopIds = setOf(providence.id),
                                            upcomingTrips = emptyList(),
                                            alertsHere = listOf(providenceTrackChangeAlert),
                                            allDataLoaded = true,
                                            hasSchedulesTodayByPattern =
                                                mapOf(routePatternPvd.id to true),
                                            alertsDownstream = emptyList(),
                                            context = context,
                                        )
                                    ),
                                    global,
                                )
                            ),
                        time,
                    )
                ),
                RouteCardData.routeCardsForStopList(
                    stopIds = listOf(providence.id),
                    globalData = global,
                    sortByDistanceFrom = providence.position,
                    schedules = ScheduleResponse(objects),
                    predictions = PredictionsStreamDataResponse(objects),
                    alerts = AlertsStreamDataResponse(objects),
                    now = time,
                    pinnedRoutes = setOf(),
                    context = context,
                ),
            )
        }
}
