package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.getPlatform
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class WorldCupServiceTest {
    @Test
    fun `scheduleUrl has the schedule`() = runTest {
        val client = HttpClient(getPlatform().httpClientEngine) { followRedirects = true }
        // apparently on iOS HTTPS is tricky, so if requesting mbta.com fails, give up instead of
        // failing
        try {
            client.get("https://mbta.com")
        } catch (_: IOException) {
            return@runTest
        }
        val response = client.get(WorldCupService.scheduleUrl)
        assertTrue(response.status.isSuccess())
        val responseBody: String = response.body()
        assertContains(responseBody, "Jun 13")
        assertContains(responseBody, "Jun 16")
        assertContains(responseBody, "Jun 19")
        assertContains(responseBody, "Jun 23")
        assertContains(responseBody, "Jun 26")
        assertContains(responseBody, "Jun 29")
        assertContains(responseBody, "Jul 9")
    }
}
