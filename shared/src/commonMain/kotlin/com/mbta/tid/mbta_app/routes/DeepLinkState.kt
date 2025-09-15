package com.mbta.tid.mbta_app.routes

import io.ktor.http.Url

public sealed class DeepLinkState {
    public data object None : DeepLinkState()

    public companion object {
        public fun from(url: String): DeepLinkState? {
            val url = Url(url)
            return when (url.encodedPath) {
                "",
                "/" -> None
                else -> {
                    println("Unhandled deep link URI $url")
                    null
                }
            }
        }
    }
}
