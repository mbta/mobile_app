package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.android.testUtils.waitUntilExactlyOneExistsDefaultTimeout
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteResult
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.repositories.IdleGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockSearchResultRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext

class RoutePickerViewTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testDisplaysLoadingIndicator() {
        val koin =
            testKoinApplication(ObjectCollectionBuilder()) { global = IdleGlobalRepository() }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Root,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasContentDescription("Loading")).assertIsDisplayed()
    }

    @Test
    fun testDisplaysFavoritesHeader() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            longName = "Red Line"
            type = RouteType.HEAVY_RAIL
        }

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Root,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add favorite stops").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun testDisplaysModePaths() {
        val objects = ObjectCollectionBuilder()

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Root,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Bus").assertCountEquals(2)
        composeTestRule.onNodeWithText("Silver Line").assertIsDisplayed()
        composeTestRule.onNodeWithText("Commuter Rail").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ferry").assertIsDisplayed()
    }

    @Test
    fun testDisplaysSubwayHeader() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            longName = "Orange Line"
            type = RouteType.HEAVY_RAIL
        }

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Root,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Subway").assertIsDisplayed()
    }

    @Test
    fun testDisplaysSubwayRoutes() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            longName = "Red Line"
            type = RouteType.HEAVY_RAIL
        }
        objects.route {
            longName = "Mattapan Trolley"
            type = RouteType.LIGHT_RAIL
        }

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Root,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Red Line").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mattapan Trolley").assertIsDisplayed()
    }

    @Test
    fun testDisplaysBus() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            shortName = "1"
            longName = "Harvard Square - Nubian Station"
            type = RouteType.BUS
        }
        objects.route {
            shortName = "71"
            longName = "Watertown Square - Harvard Station"
            type = RouteType.BUS
        }
        objects.route {
            id = "741"
            shortName = "SL1"
            longName = "Logan Airport Terminals - South Station"
            type = RouteType.BUS
        }
        objects.route {
            longName = "Mattapan Trolley"
            type = RouteType.LIGHT_RAIL
        }

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Bus,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Harvard Square - Nubian Station").assertIsDisplayed()
        composeTestRule.onNodeWithText("Watertown Square - Harvard Station").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Logan Airport Terminals - South Station")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Mattapan Trolley").assertIsNotDisplayed()
    }

    @Test
    fun testDisplaysSilverLineRoutes() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            shortName = "66"
            longName = "Harvard Square - Nubian Station"
            type = RouteType.BUS
        }
        objects.route {
            id = "741"
            shortName = "SL1"
            longName = "Logan Airport Terminals - South Station"
            type = RouteType.BUS
        }

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Silver,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Harvard Square - Nubian Station").assertIsNotDisplayed()
        composeTestRule
            .onNodeWithText("Logan Airport Terminals - South Station")
            .assertIsDisplayed()
    }

    @Test
    fun testDisplaysCommuterRailRoutes() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            longName = "Providence/Stoughton Line"
            type = RouteType.COMMUTER_RAIL
        }

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.CommuterRail,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Providence/Stoughton Line").assertIsDisplayed()
    }

    @Test
    fun testDisplaysFerryRoutes() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            longName = "Lynn Ferry"
            type = RouteType.FERRY
        }

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Ferry,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Lynn Ferry").assertIsDisplayed()
    }

    @Test
    fun testRouteSelection() {
        val objects = ObjectCollectionBuilder()
        val route =
            objects.route {
                longName = "Orange Line"
                type = RouteType.HEAVY_RAIL
            }

        var selectedRouteId: String? = null
        var selectedContext: RouteDetailsContext? = null

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Root,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { routeId, context ->
                        selectedRouteId = routeId
                        selectedContext = context
                    },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Orange Line").performClick()
        composeTestRule.waitForIdle()

        assertEquals(route.id, selectedRouteId)
        assertEquals(RouteDetailsContext.Favorites, selectedContext)
    }

    @Test
    fun testBackButton() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            longName = "Bus"
            type = RouteType.BUS
        }

        var backCalled = false

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Bus,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = { backCalled = true },
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        assertTrue(backCalled)
    }

    @Test
    fun testDoneButton() {
        val objects = ObjectCollectionBuilder()
        objects.route {
            longName = "Green Line"
            type = RouteType.LIGHT_RAIL
        }

        var closeCalled = false

        val koin =
            testKoinApplication(objects) { global = MockGlobalRepository(GlobalResponse(objects)) }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Root,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = { closeCalled = true },
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.waitForIdle()

        assertTrue(closeCalled)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFilterInputRequests() {
        val objects = ObjectCollectionBuilder()
        val route1 =
            objects.route {
                shortName = "1"
                longName = "Harvard Square - Nubian Station"
                type = RouteType.BUS
            }
        val route71 =
            objects.route {
                shortName = "71"
                longName = "Watertown Square - Harvard Station"
                type = RouteType.BUS
            }

        val koin =
            testKoinApplication(objects) {
                global = MockGlobalRepository(GlobalResponse(objects))
                searchResults =
                    MockSearchResultRepository(routeResults = listOf(RouteResult(route = route1)))
            }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Bus,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(route71.longName).assertIsDisplayed()
        composeTestRule.onNodeWithText("Filter routes").performClick().performTextInput("query")
        composeTestRule.waitForIdle()
        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("To find stops, select a route first")
        )
        composeTestRule.onNodeWithText(route1.longName).assertIsDisplayed()
        composeTestRule.onNodeWithText(route71.longName).assertIsNotDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testNoFilterResults() {
        val objects = ObjectCollectionBuilder()
        val route1 =
            objects.route {
                shortName = "1"
                longName = "Harvard Square - Nubian Station"
                type = RouteType.BUS
            }
        val route71 =
            objects.route {
                shortName = "71"
                longName = "Watertown Square - Harvard Station"
                type = RouteType.BUS
            }

        val koin =
            testKoinApplication(objects) {
                global = MockGlobalRepository(GlobalResponse(objects))
                searchResults = MockSearchResultRepository(routeResults = emptyList())
            }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Bus,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = {},
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(route1.longName).assertIsDisplayed()
        composeTestRule.onNodeWithText(route71.longName).assertIsDisplayed()
        composeTestRule.onNodeWithText("Filter routes").performClick().performTextInput("query")
        composeTestRule.waitForIdle()

        composeTestRule.waitUntilExactlyOneExistsDefaultTimeout(
            hasText("To find stops, select a route first")
        )
        composeTestRule.onNodeWithText("No matching bus routes").assertIsDisplayed()
        composeTestRule.onNodeWithText(route1.longName).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(route71.longName).assertIsNotDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testFilterFocusCallback() {
        val objects = ObjectCollectionBuilder()

        val koin =
            testKoinApplication(objects) {
                global = MockGlobalRepository(GlobalResponse(objects))
                searchResults = MockSearchResultRepository(routeResults = emptyList())
            }
        val errorBannerVM = ErrorBannerViewModel(errorRepository = MockErrorBannerStateRepository())

        var filterExpanded = false

        composeTestRule.setContent {
            KoinContext(koin.koin) {
                RoutePickerView(
                    path = RoutePickerPath.Bus,
                    context = RouteDetailsContext.Favorites,
                    onOpenPickerPath = { _, _ -> },
                    onOpenRouteDetails = { _, _ -> },
                    onRouteSearchExpandedChange = { filterExpanded = it },
                    onBack = {},
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Filter routes").performClick()
        composeTestRule.waitForIdle()

        assert(filterExpanded)
    }
}
