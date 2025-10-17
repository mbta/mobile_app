package com.mbta.tid.mbta_app.repositories

import com.mbta.tid.mbta_app.model.SubscriptionRequest
import com.mbta.tid.mbta_app.model.UpdateAccessibilityRequest
import com.mbta.tid.mbta_app.model.WriteSubscriptionsRequest
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.network.MobileBackendClient
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.path
import kotlin.getValue
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public interface ISubscriptionsRepository {
    public suspend fun updateSubscriptions(
        fcmToken: String,
        subscriptions: List<SubscriptionRequest>,
    )

    public suspend fun updateAccessibility(fcmToken: String, includeAccessibility: Boolean)
}

internal class SubscriptionsRepository : ISubscriptionsRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun updateSubscriptions(
        fcmToken: String,
        subscriptions: List<SubscriptionRequest>,
    ) {
        val requestBody = WriteSubscriptionsRequest(fcmToken, subscriptions)
        ApiResult.runCatching {
            mobileBackendClient
                .post {
                    url { path("api/notifications/subscriptions/write") }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
                .body()
        }
    }

    override suspend fun updateAccessibility(fcmToken: String, includeAccessibility: Boolean) {
        val requestBody = UpdateAccessibilityRequest(fcmToken, includeAccessibility)
        ApiResult.runCatching {
            mobileBackendClient
                .post {
                    url { path("api/notifications/subscriptions/accessibility") }
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }
                .body()
        }
    }
}

public class MockSubscriptionsRepository(
    public val onUpdateSubscriptions: (String, List<SubscriptionRequest>) -> Unit = { _, _ -> },
    public val onUpdateAccessibility: (String, Boolean) -> Unit = { _, _ -> },
) : ISubscriptionsRepository {
    override suspend fun updateSubscriptions(
        fcmToken: String,
        subscriptions: List<SubscriptionRequest>,
    ) {
        onUpdateSubscriptions(fcmToken, subscriptions)
    }

    override suspend fun updateAccessibility(fcmToken: String, includeAccessibility: Boolean) {
        onUpdateAccessibility(fcmToken, includeAccessibility)
    }
}
