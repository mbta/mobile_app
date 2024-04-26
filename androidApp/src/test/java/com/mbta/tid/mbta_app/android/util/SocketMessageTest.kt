package com.mbta.tid.mbta_app.android.util

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.phoenixframework.Message

class SocketMessageTest {
    @Test
    fun testDecode() {
        // examples from https://hexdocs.pm/phoenix/writing_a_channels_client.html
        assertEquals(
            Message(
                "0",
                "0",
                "miami:weather",
                "phx_join",
                buildJsonObject { put("some", "param") }
            ),
            decodeMessage("""["0","0","miami:weather","phx_join",{"some":"param"}]""")
        )

        assertEquals(
            Message(null, "1", "miami:weather", "phx_leave"),
            decodeMessage("""[null,"1","miami:weather","phx_leave",{}]""")
        )

        assertEquals(
            Message(null, "2", "phoenix", "heartbeat"),
            decodeMessage("""[null,"2","phoenix","heartbeat",{}]""")
        )

        assertEquals(
            Message(
                null,
                "3",
                "miami:weather",
                "report_emergency",
                buildJsonObject { put("category", "sharknado") }
            ),
            decodeMessage(
                """[null,"3","miami:weather","report_emergency",{"category":"sharknado"}]"""
            )
        )

        // data observed in practice
        assertEquals(
            Message(
                "0",
                "0",
                "predictions:stops",
                "phx_reply",
                buildJsonObject {
                    put("status", "ok")
                    putJsonObject("response") {}
                }
            ),
            decodeMessage(
                """["0","0","predictions:stops","phx_reply",{"status":"ok","response":{}}]"""
            )
        )

        assertEquals(
            Message("0", "", "predictions:stops", "stream_data"),
            decodeMessage("""["0",null,"predictions:stops","stream_data",{}]""")
        )
    }
}
