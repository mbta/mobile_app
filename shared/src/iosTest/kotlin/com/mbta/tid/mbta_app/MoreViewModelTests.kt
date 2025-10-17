package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.model.morePage.MoreItem
import com.mbta.tid.mbta_app.model.morePage.MoreSection
import com.mbta.tid.mbta_app.viewModel.MoreViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class MoreViewModelTests {

    @Test
    fun testLocalizedFeedbackLink() {
        val vm = MoreViewModel()
        val sections = vm.getSections("es", {})
        assertEquals(
            "https://mbta.com/appfeedback?lang=es-US",
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
        val vm = MoreViewModel()
        val sections = vm.getSections("", {})
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
