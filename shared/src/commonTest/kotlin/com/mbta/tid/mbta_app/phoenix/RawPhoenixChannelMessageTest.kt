package com.mbta.tid.mbta_app.phoenix

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class RawPhoenixChannelMessageTest {
    @Test
    fun testSerialization() {
        // examples from https://hexdocs.pm/phoenix/writing_a_channels_client.html
        testPair(
            """["0","0","miami:weather","phx_join",{"some":"param"}]""",
            RawPhoenixChannelMessage(
                "0",
                "0",
                "miami:weather",
                "phx_join",
                buildJsonObject { put("some", "param") }
            )
        )

        testPair(
            """[null,"1","miami:weather","phx_leave",{}]""",
            RawPhoenixChannelMessage(null, "1", "miami:weather", "phx_leave")
        )

        testPair(
            """[null,"2","phoenix","heartbeat",{}]""",
            RawPhoenixChannelMessage(null, "2", "phoenix", "heartbeat")
        )

        testPair(
            """[null,"3","miami:weather","report_emergency",{"category":"sharknado"}]""",
            RawPhoenixChannelMessage(
                null,
                "3",
                "miami:weather",
                "report_emergency",
                buildJsonObject { put("category", "sharknado") }
            )
        )

        // data observed in practice
        testPair(
            """["0","0","predictions:stops","phx_reply",{"status":"ok","response":{}}]""",
            RawPhoenixChannelMessage(
                "0",
                "0",
                "predictions:stops",
                "phx_reply",
                buildJsonObject {
                    put("status", "ok")
                    putJsonObject("response") {}
                }
            )
        )

        testPair(
            """["0",null,"predictions:stops","stream_data",{}]""",
            RawPhoenixChannelMessage("0", null, "predictions:stops", "stream_data")
        )
    }

    private fun testPair(string: String, rawPhoenixChannelMessage: RawPhoenixChannelMessage) {
        assertEquals(string, Json.encodeToString(rawPhoenixChannelMessage))
        assertEquals(rawPhoenixChannelMessage, Json.decodeFromString(string))
    }
}
