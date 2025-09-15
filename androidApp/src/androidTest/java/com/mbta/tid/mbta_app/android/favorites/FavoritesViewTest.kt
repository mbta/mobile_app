package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.response.AlertsStreamDataResponse
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSentryRepository
import com.mbta.tid.mbta_app.viewModel.ErrorBannerViewModel
import com.mbta.tid.mbta_app.viewModel.FavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.MockFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import dev.mokkery.MockMode
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.resetCalls
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlin.time.Clock
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class FavoritesViewTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testShowsToast() = runBlocking {
        val favoritesVM =
            MockFavoritesViewModel(
                initialState =
                    FavoritesViewModel.State(
                        false,
                        emptySet(),
                        true,
                        emptyList(),
                        emptyList(),
                        null,
                    )
            )
        val toastVM = mock<IToastViewModel>(MockMode.autofill)

        val objects = ObjectCollectionBuilder()

        composeTestRule.setContent {
            FavoritesView(
                openSheetRoute = {},
                favoritesViewModel = favoritesVM,
                errorBannerViewModel =
                    ErrorBannerViewModel(
                        errorRepository = MockErrorBannerStateRepository(),
                        MockSentryRepository(),
                        Clock.System,
                    ),
                toastViewModel = toastVM,
                alertData = AlertsStreamDataResponse(objects),
                globalResponse = GlobalResponse(objects),
                targetLocation = null,
                setLastLocation = { _ -> },
                setIsTargeting = { _ -> },
            )
        }

        verify(VerifyMode.exhaustiveOrder) {
            toastVM.showToast(
                ToastViewModel.Toast(
                    "Favorite stops replaces the prior starred routes feature.",
                    ToastViewModel.Duration.Indefinite,
                    any(),
                )
            )
        }
        resetCalls(toastVM)

        composeTestRule.onNodeWithText("Add stops").performClick()

        verify(VerifyMode.exhaustiveOrder) { toastVM.hideToast() }
    }
}
