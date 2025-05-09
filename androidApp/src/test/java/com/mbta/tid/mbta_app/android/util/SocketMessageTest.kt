package com.mbta.tid.mbta_app.android.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.phoenixframework.Message

class SocketMessageTest {
    @Test
    fun testDecode() {
        // examples from https://hexdocs.pm/phoenix/writing_a_channels_client.html
        assertEquals(
            Message("0", "0", "miami:weather", "phx_join", emptyMap(), """{"some":"param"}"""),
            decodeMessage("""["0","0","miami:weather","phx_join",{"some":"param"}]"""),
        )

        assertEquals(
            Message(null, "1", "miami:weather", "phx_leave", emptyMap(), "{}"),
            decodeMessage("""[null,"1","miami:weather","phx_leave",{}]"""),
        )

        assertEquals(
            Message(null, "2", "phoenix", "heartbeat", emptyMap(), "{}"),
            decodeMessage("""[null,"2","phoenix","heartbeat",{}]"""),
        )

        assertEquals(
            Message(
                null,
                "3",
                "miami:weather",
                "report_emergency",
                emptyMap(),
                """{"category":"sharknado"}""",
            ),
            decodeMessage(
                """[null,"3","miami:weather","report_emergency",{"category":"sharknado"}]"""
            ),
        )

        // data observed in practice
        assertEquals(
            Message("0", "0", "predictions:stops", "phx_reply", mapOf("status" to "ok"), "{}"),
            decodeMessage(
                """["0","0","predictions:stops","phx_reply",{"status":"ok","response":{}}]"""
            ),
        )

        assertEquals(
            Message("0", "", "predictions:stops", "stream_data", emptyMap(), "{}"),
            decodeMessage("""["0",null,"predictions:stops","stream_data",{}]"""),
        )
    }
}
