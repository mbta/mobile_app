package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.PlatformType
import com.mbta.tid.mbta_app.repositories.Settings
import io.ktor.http.URLBuilder
import io.ktor.util.appendAll

public fun feedbackFormUrl(
    baseUrl: String,
    translation: String,
    version: String?,
    platform: PlatformType,
    settings: Map<Settings, Boolean>,
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
        .appendAll(settings.entries.associate { it.key.name to it.value.toString() })
        .build()

    return builder.build().toString()
}
