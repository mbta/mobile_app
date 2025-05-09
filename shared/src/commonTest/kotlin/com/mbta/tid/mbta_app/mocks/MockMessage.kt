package com.mbta.tid.mbta_app.mocks

import com.mbta.tid.mbta_app.network.PhoenixMessage

class MockMessage(
    override val subject: String = "",
    override val body: Map<String, Any> = emptyMap(),
    override val jsonBody: String? = null,
) : PhoenixMessage
