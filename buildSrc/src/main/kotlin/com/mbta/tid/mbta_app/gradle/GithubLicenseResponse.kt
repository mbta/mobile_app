package com.mbta.tid.mbta_app.gradle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GithubLicenseResponse(
    val encoding: String,
    val content: String,
    val license: KnownLicense
) {
    @Serializable data class KnownLicense(@SerialName("spdx_id") val spdxId: String)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(jsonString: String): GithubLicenseResponse = json.decodeFromString(jsonString)
    }
}
