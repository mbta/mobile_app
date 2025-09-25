package com.mbta.tid.mbta_app.android.pages

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mbta.tid.mbta_app.android.loadKoinMocks
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.TestData
import org.junit.Rule
import org.junit.Test

class SaveFavoritePageTests {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testCloses() {
        var backCalled = false

        val objects = TestData.clone()
        val route = objects.getRoute("Red")
        val stop = objects.getStop("70069")

        loadKoinMocks(objects)

        composeTestRule.setContent {
            SaveFavoritePage(
                route.id,
                stop.id,
                1,
                EditFavoritesContext.StopDetails,
                { backCalled = true },
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()
        assert(backCalled)
    }
}
