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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class VehicleRepositoryTests {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testChannelClearedBeforeJoin() = runTest {
        val socket = mock<PhoenixSocket>(MockMode.autofill)
        val channel = mock<PhoenixChannel>(MockMode.autofill)
        val push = mock<PhoenixPush>(MockMode.autofill)
        val vehicleRepo = VehicleRepository(socket, StandardTestDispatcher(testScheduler))
        every { channel.attach() } returns push
        every { push.receive(any(), any()) } returns push
        every { socket.getChannel(any(), any()) } returns channel
        vehicleRepo.channel = channel
        vehicleRepo.connect(vehicleId = "Test", onReceive = {})
        advanceUntilIdle()
        verify { channel.detach() }
    }
}
