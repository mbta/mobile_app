package com.mbta.tid.mbta_app.android.routePicker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.component.ErrorBannerViewModel
import com.mbta.tid.mbta_app.android.testKoinApplication
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteDetailsContext
import com.mbta.tid.mbta_app.model.routeDetailsPage.RoutePickerPath
import com.mbta.tid.mbta_app.repositories.IdleGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
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
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add favorite stops").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
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
                    onClose = {},
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Subway").assertIsDisplayed()
    }

    @Test
    fun testDisplaysRoutes() {
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
    fun testCancelButton() {
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
                    onClose = { closeCalled = true },
                    errorBannerViewModel = errorBannerVM,
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        assertTrue(closeCalled)
    }
}
