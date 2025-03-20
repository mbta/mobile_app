package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.network.PhoenixChannel
import com.mbta.tid.mbta_app.network.PhoenixPush
import com.mbta.tid.mbta_app.network.PhoenixSocket
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import kotlin.test.Test

class TripPredictionsRepositoryTests {

    @Test
    fun testChannelClearedBeforeJoin() {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val tripPredictionsRepo = TripPredictionsRepository(socket)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        every { channel.attach() } returns mock<PhoenixPush>(MockMode.autofill)
        every { socket.getChannel(any(), any()) } returns mock<PhoenixChannel>(MockMode.autofill)
        tripPredictionsRepo.channel = channel
        tripPredictionsRepo.connect(tripId = "Test", onReceive = { })
        verify { tripPredictionsRepo.disconnect() }
        verify { channel.detach() }
    }

}
