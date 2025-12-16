package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.PlatformType
import io.ktor.http.URLBuilder
import io.ktor.util.appendAll

public fun feedbackFormUrl(
    baseUrl: String,
    translation: String,
    version: String?,
    platform: PlatformType,
): String {
    val builder = URLBuilder(baseUrl)
    builder.parameters
        .appendAll(
            mapOf(
                Pair("language", translation),
                Pair("version", version ?: "null"),
                Pair(
                    "platform",
                    when (platform) {
                        PlatformType.iOS -> "iOS"
                        else -> "Android"
                    },
                ),
            )
        )
        .build()

    return builder.build().toString()
}
