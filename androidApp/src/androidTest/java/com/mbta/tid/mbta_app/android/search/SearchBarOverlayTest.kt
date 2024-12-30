package com.mbta.tid.mbta_app.android.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.SearchResults
import com.mbta.tid.mbta_app.model.StopResult
import com.mbta.tid.mbta_app.model.StopResultRoute
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.ISearchResultRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.KoinTest

@ExperimentalTestApi
@ExperimentalMaterial3Api
class SearchBarOverlayTest : KoinTest {
    val koinApplication = koinApplication {
        modules(
            module {
                single<IGlobalRepository> { MockGlobalRepository() }
                single<ISearchResultRepository> {
                    object : ISearchResultRepository {
                        override suspend fun getSearchResults(
                            query: String
                        ): ApiResult<SearchResults>? {
                            return ApiResult.Ok(
                                SearchResults(
                                    routes = emptyList(),
                                    stops =
                                        listOf(
                                            StopResult(
                                                id = "stopId",
                                                rank = 2,
                                                name = "stopName",
                                                zone = "stopZone",
                                                isStation = false,
                                                routes =
                                                    listOf(
                                                        StopResultRoute(
                                                            type = RouteType.BUS,
                                                            icon = "routeIcon",
                                                        )
                                                    )
                                            )
                                        )
                                )
                            )
                        }
                    }
                }
            }
        )
    }

    @get:Rule var composeTestRule = createComposeRule()

    @Test
    fun testSearchBarOverlayBehavesCorrectly() = runTest {
        val navigated = mutableStateOf(false)

        composeTestRule.setContent {
            KoinContext(koinApplication.koin) {
                val focusRequester = remember { FocusRequester() }
                SearchBarOverlay(
                    onStopNavigation = { navigated.value = true },
                    currentNavEntry = null,
                    inputFieldFocusRequester = focusRequester,
                ) {
                    Text("Content")
                }
            }
        }

        composeTestRule.onNodeWithText("Content").assertExists()
        val searchNode = composeTestRule.onNodeWithText("Search by stop")
        searchNode.assertExists()
        searchNode.requestFocus()
        composeTestRule.awaitIdle()

        searchNode.performTextInput("sto")
        composeTestRule.waitUntilAtLeastOneExists(hasText("stopName"))
        composeTestRule.onNodeWithText("stopName").performClick()
        composeTestRule.waitUntil { navigated.value }
    }
}
