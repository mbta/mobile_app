package com.mbta.tid.mbta_app.viewModel

import app.cash.turbine.test
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.history.Visit
import com.mbta.tid.mbta_app.history.VisitHistory
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RoutePillSpec
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.IVisitHistoryRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockSearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.repositories.MockVisitHistoryRepository
import com.mbta.tid.mbta_app.usecases.VisitHistoryUsecase
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.test.runTest

class SearchViewModelTest {
    @Test
    fun testSearchResults() = runTest {
        val objects = ObjectCollectionBuilder()
        val visitedStop = objects.stop { name = "visitedStopName" }
        val searchedStop = objects.stop { name = "stopName" }

        val searchResults =
            SearchResults(
                routes = emptyList(),
                stops =
                    listOf(
                        StopResult(
                            id = searchedStop.id,
                            rank = 2,
                            name = searchedStop.name,
                            zone = "stopZone",
                            isStation = false,
                            routes = emptyList(),
                        )
                    ),
            )

        val visitHistoryRepo =
            object : IVisitHistoryRepository {
                val visitHistory = VisitHistory().apply { add(Visit.StopVisit(visitedStop.id)) }

                override suspend fun getVisitHistory(): VisitHistory {
                    delay(10.milliseconds)
                    return visitHistory
                }

                override suspend fun setVisitHistory(visits: VisitHistory) {}
            }

        val searchVM =
            SearchViewModel(
                MockAnalytics(),
                MockGlobalRepository(GlobalResponse(objects)),
                object : ISearchResultRepository {
                    override suspend fun getRouteFilterResults(
                        query: String,
                        lineIds: List<String>?,
                        routeTypes: List<RouteType>?,
                    ): ApiResult<SearchResults>? {
                        fail("Route search should not be called here")
                    }

                    override suspend fun getSearchResults(query: String): ApiResult<SearchResults> {
                        delay(10.milliseconds)
                        return ApiResult.Ok(searchResults)
                    }
                },
                MockSentryRepository(),
                VisitHistoryUsecase(visitHistoryRepo),
            )

        testViewModelFlow(searchVM).test {
            assertEquals(SearchViewModel.State.Loading, awaitItem())
            searchVM.setQuery("")
            assertEquals(
                SearchViewModel.State.RecentStops(
                    listOf(
                        SearchViewModel.StopResult(
                            visitedStop.id,
                            false,
                            visitedStop.name,
                            emptyList(),
                        )
                    )
                ),
                awaitItem(),
            )
            searchVM.setQuery("query")
            assertEquals(SearchViewModel.State.Loading, awaitItem())
            assertEquals(
                SearchViewModel.State.Results(
                    listOf(
                        SearchViewModel.StopResult(
                            searchedStop.id,
                            false,
                            searchedStop.name,
                            emptyList(),
                        )
                    ),
                    emptyList(),
                ),
                awaitItem(),
            )
        }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun testRoutePills() = runTest {
        val objects = TestData.clone()
        val station =
            objects.stop {
                name = "Some Station"
                locationType = LocationType.STATION
            }
        val stop =
            objects.stop {
                name = "Some Stop"
                locationType = LocationType.STOP
            }

        val sl1 = objects.getRoute("741")
        objects.routePattern(sl1) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(station.id, stop.id) }
        }
        val sl2 = objects.getRoute("742")
        objects.routePattern(sl2) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(station.id, stop.id) }
        }

        val crFitchburg = objects.getRoute("CR-Fitchburg")
        objects.routePattern(crFitchburg) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(station.id, stop.id) }
        }
        val crHaverhill = objects.getRoute("CR-Haverhill")
        objects.routePattern(crHaverhill) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(station.id, stop.id) }
        }

        val bus15 = objects.getRoute("15")
        objects.routePattern(bus15) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(station.id, stop.id) }
        }
        val bus67 = objects.getRoute("67")
        objects.routePattern(bus67) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(station.id, stop.id) }
        }

        val redLine = objects.getRoute("Red")
        objects.routePattern(redLine) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(station.id, stop.id) }
        }

        val viewModel =
            SearchViewModel(
                MockAnalytics(),
                MockGlobalRepository(GlobalResponse(objects)),
                MockSearchResultRepository(),
                MockSentryRepository(),
                VisitHistoryUsecase(
                    MockVisitHistoryRepository(
                        VisitHistory().apply {
                            add(Visit.StopVisit(stop.id))
                            add(Visit.StopVisit(station.id))
                        }
                    )
                ),
            )

        val state =
            testViewModelFlow(viewModel)
                .timeout(10.seconds)
                .filterIsInstance<SearchViewModel.State.RecentStops>()
                .first()

        assertEquals(
            SearchViewModel.State.RecentStops(
                listOf(
                    SearchViewModel.StopResult(
                        id = station.id,
                        isStation = true,
                        name = station.name,
                        listOf(
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "DA291C",
                                RoutePillSpec.Content.Text("RL"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Capsule,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    redLine.label,
                                    redLine.type,
                                    isOnly = true,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "7C878E",
                                RoutePillSpec.Content.ModeImage(RouteType.BUS),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Rectangle,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    "Silver Line",
                                    RouteType.BUS,
                                    isOnly = false,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "80276C",
                                RoutePillSpec.Content.Text("CR"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Capsule,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    "Commuter Rail",
                                    RouteType.COMMUTER_RAIL,
                                    isOnly = false,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "000000",
                                routeColor = "FFC72C",
                                RoutePillSpec.Content.ModeImage(RouteType.BUS),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Rectangle,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    null,
                                    RouteType.BUS,
                                    isOnly = false,
                                ),
                            ),
                        ),
                    ),
                    SearchViewModel.StopResult(
                        id = stop.id,
                        isStation = false,
                        name = stop.name,
                        listOf(
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "DA291C",
                                RoutePillSpec.Content.Text("RL"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Capsule,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    redLine.label,
                                    redLine.type,
                                    isOnly = true,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "7C878E",
                                RoutePillSpec.Content.Text("SL1"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Rectangle,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    "SL1",
                                    RouteType.BUS,
                                    isOnly = true,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "7C878E",
                                RoutePillSpec.Content.Text("SL2"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Rectangle,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    "SL2",
                                    RouteType.BUS,
                                    isOnly = true,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "80276C",
                                RoutePillSpec.Content.Text("CR"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Capsule,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    crFitchburg.longName,
                                    RouteType.COMMUTER_RAIL,
                                    isOnly = true,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "FFFFFF",
                                routeColor = "80276C",
                                RoutePillSpec.Content.Text("CR"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Capsule,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    crHaverhill.longName,
                                    RouteType.COMMUTER_RAIL,
                                    isOnly = true,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "000000",
                                routeColor = "FFC72C",
                                RoutePillSpec.Content.Text("15"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Rectangle,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    "15",
                                    RouteType.BUS,
                                    isOnly = true,
                                ),
                            ),
                            RoutePillSpec(
                                textColor = "000000",
                                routeColor = "FFC72C",
                                RoutePillSpec.Content.Text("67"),
                                RoutePillSpec.Height.Small,
                                RoutePillSpec.Width.Flex,
                                RoutePillSpec.Shape.Rectangle,
                                RoutePillSpec.ContentDescription.StopSearchResultRoute(
                                    "67",
                                    RouteType.BUS,
                                    isOnly = true,
                                ),
                            ),
                        ),
                    ),
                )
            ),
            state,
        )
    }
}
