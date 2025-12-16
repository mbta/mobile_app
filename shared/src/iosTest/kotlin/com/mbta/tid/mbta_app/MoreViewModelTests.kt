package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.repositories.MockSubscriptionsRepository
import com.mbta.tid.mbta_app.viewModel.MoreViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers

class MoreViewModelTests {

    @Test
    fun testLocalizedFeedbackLink() {
        val vm = MoreViewModel(Dispatchers.Default, MockSubscriptionsRepository())
        val sections = vm.getSections("es", "1.2.3", {})
        assertEquals(
            "https://mbta.com/appfeedback?language=es&version=1.2.3&platform=iOS",
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
        val sections = vm.getSections("", "", {})
        assertEquals(
            "https://apps.apple.com/us/app/mbta-mticket/id560487958",
            sections
                .first { it.id == MoreSection.Category.Resources }
                .items
                .filterIsInstance<MoreItem.Link>()
                .last()
                .url,
        )
    }
}
