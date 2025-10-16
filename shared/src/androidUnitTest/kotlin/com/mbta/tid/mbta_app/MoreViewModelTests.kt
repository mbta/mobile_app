package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.MockSubscriptionsRepository
import com.mbta.tid.mbta_app.viewModel.MoreViewModel
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import org.junit.Test

class MoreViewModelTests {

    @Test
    fun testLocalizedFeedbackLink() {
        val vm = MoreViewModel(Dispatchers.Default, MockSubscriptionsRepository())
        val sections = vm.getSections("es", {})
        assertEquals(
            "https://mbta.com/androidappfeedback?lang=es-US",
            sections
                .first { it.id == MoreSection.Category.Feedback }
                .items
                .filterIsInstance<MoreItem.Link>()
                .first()
                .url,
        )
    }

    @Test
    fun testMTicketURL() {
        val vm = MoreViewModel(Dispatchers.Default, MockSubscriptionsRepository())
        val sections = vm.getSections("", {})
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.mbta.mobileapp",
            sections
                .first { it.id == MoreSection.Category.Resources }
                .items
                .filterIsInstance<MoreItem.Link>()
                .last()
                .url,
        )
    }
}
