package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.AppVariant
import com.mbta.tid.mbta_app.mocks.mockJsonPersistence
import com.mbta.tid.mbta_app.model.Facility
import com.mbta.tid.mbta_app.model.Line
import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.Trip
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest

class GlobalRepositoryTest : KoinTest {
    @AfterTest
    fun teardown() {
        stopKoin()
    }

    @Test
    fun testGetGlobalData() {
        val mockEngine = MockEngine { _ ->
            respond(
                content =
                    ByteReadChannel(
                        """
{
  "facilities": {
    "808": {
      "id": "808",
      "type": "elevator",
      "long_name": "Park Street Elevator 808 (Red Line center platform to Government Center & North platform, Winter Street Concourse)",
      "short_name": "Red Line center platform to Government Center & North platform, Winter Street Concourse"
    }
  },
  "lines": {
    "line-Green": {
      "id": "line-Green",
      "color": "00843D",
      "long_name": "Green Line",
      "short_name": "",
      "sort_order": 10032,
      "text_color": "FFFFFF"
    }
  },
  "routes": {
    "Shuttle-AirportGovernmentCenterLocal": {
      "id": "Shuttle-AirportGovernmentCenterLocal",
      "type": "bus",
      "color": "FFC72C",
      "direction_names": [
        "West",
        "East"
      ],
      "direction_destinations": [
        "Bowdoin",
        "Wonderland"
      ],
      "listed_route" : false,
      "long_name": "Airport - Government Center (Local)",
      "short_name": "Blue Line Shuttle",
      "sort_order": 60208,
      "text_color": "000000",
      "route_pattern_ids": null
    }
  },
  "route_patterns": {
    "39-3-0": {
      "id": "39-3-0",
      "name": "Back Bay Station - Forest Hills Station",
      "direction_id": 0,
      "route_id": "39",
      "sort_order": 503900000,
      "canonical": false,
      "typicality": "typical",
      "representative_trip_id": "61945832"
    }
  },
  "stops": {
    "3992": {
      "id": "3992",
      "name": "S Franklin St @ Emery St",
      "longitude": -71.011556,
      "latitude": 42.125615,
      "child_stop_ids": [],
      "connecting_stop_ids": [],
      "location_type": "stop",
      "vehicle_type": "bus"
    }
  },
  "trips": {
    "62145526_2": {
      "id": "62145526_2",
      "direction_id": 0,
      "route_id": "37",
      "route_pattern_id": "37-D-0",
      "stop_ids": [
        "10642",
        "596"
      ],
      "headsign": "LaGrange & Corey",
      "shape_id": "370137-2"
    }
  },
  "pattern_ids_by_stop": {
    "3992": [
      "230-3-1",
      "230-5-1"
    ]
  }
}
                    """
                            .trimIndent()
                    ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        startKoin {
            modules(
                module {
                    single { MobileBackendClient(mockEngine, AppVariant.Staging) }
                    single { mockJsonPersistence() }
                }
            )
        }
        runBlocking {
            val repo = GlobalRepository()
            assertNull(repo.state.value)
            val response = repo.getGlobalData()
            val facility =
                Facility(
                    id = "808",
                    longName =
                        "Park Street Elevator 808 (Red Line center platform to Government Center & North platform, Winter Street Concourse)",
                    shortName =
                        "Red Line center platform to Government Center & North platform, Winter Street Concourse",
                    type = Facility.Type.Elevator,
                )
            val line =
                Line(
                    id = "line-Green",
                    color = "00843D",
                    longName = "Green Line",
                    shortName = "",
                    sortOrder = 10032,
                    textColor = "FFFFFF",
                )
            val route =
                Route(
                    id = "Shuttle-AirportGovernmentCenterLocal",
                    type = RouteType.BUS,
                    color = "FFC72C",
                    directionNames = listOf("West", "East"),
                    directionDestinations = listOf("Bowdoin", "Wonderland"),
                    isListedRoute = false,
                    longName = "Airport - Government Center (Local)",
                    shortName = "Blue Line Shuttle",
                    sortOrder = 60208,
                    textColor = "000000",
                    routePatternIds = null,
                )
            val routePattern =
                RoutePattern(
                    id = "39-3-0",
                    name = "Back Bay Station - Forest Hills Station",
                    directionId = 0,
                    routeId = "39",
                    sortOrder = 503900000,
                    typicality = RoutePattern.Typicality.Typical,
                    representativeTripId = "61945832",
                )
            val stop =
                Stop(
                    id = "3992",
                    name = "S Franklin St @ Emery St",
                    longitude = -71.011556,
                    latitude = 42.125615,
                    vehicleType = RouteType.BUS,
                    childStopIds = emptyList(),
                    connectingStopIds = emptyList(),
                    locationType = LocationType.STOP,
                )
            val trip =
                Trip(
                    id = "62145526_2",
                    directionId = 0,
                    routeId = "37",
                    routePatternId = "37-D-0",
                    stopIds = listOf("10642", "596"),
                    headsign = "LaGrange & Corey",
                    shapeId = "370137-2",
                )
            val expectedResponse =
                GlobalResponse(
                    facilities = mapOf("808" to facility),
                    lines = mapOf("line-Green" to line),
                    patternIdsByStop = mapOf("3992" to listOf("230-3-1", "230-5-1")),
                    routes = mapOf("Shuttle-AirportGovernmentCenterLocal" to route),
                    routePatterns = mapOf("39-3-0" to routePattern),
                    stops = mapOf("3992" to stop),
                    trips = mapOf("62145526_2" to trip),
                )
            assertEquals(ApiResult.Ok(expectedResponse), response)
            assertEquals(expectedResponse, repo.state.value)
        }
    }
}
