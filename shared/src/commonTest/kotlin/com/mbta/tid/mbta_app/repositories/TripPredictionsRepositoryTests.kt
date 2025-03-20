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
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val tripPredictionsRepo = TripPredictionsRepository(socket)
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        tripPredictionsRepo.connect(tripId = "Test", onReceive = { })
        verify { tripPredictionsRepo.disconnect() }
        verify { channel.detach() }
    }

}
