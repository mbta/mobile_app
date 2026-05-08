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
        locale: String,
    )

    public suspend fun updateAccessibility(
        fcmToken: String,
        includeAccessibility: Boolean,
        locale: String,
    )
}

internal class SubscriptionsRepository : ISubscriptionsRepository, KoinComponent {

    private val mobileBackendClient: MobileBackendClient by inject()

    override suspend fun updateSubscriptions(
        fcmToken: String,
        subscriptions: List<SubscriptionRequest>,
        locale: String,
    ) {
        val requestBody = WriteSubscriptionsRequest(fcmToken, subscriptions, locale)
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

    override suspend fun updateAccessibility(
        fcmToken: String,
        includeAccessibility: Boolean,
        locale: String,
    ) {
        val requestBody = UpdateAccessibilityRequest(fcmToken, includeAccessibility, locale)
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
    public val onUpdateSubscriptions: (String, List<SubscriptionRequest>, String) -> Unit =
        { _, _, _ ->
        },
    public val onUpdateAccessibility: (String, Boolean, String) -> Unit = { _, _, _ -> },
) : ISubscriptionsRepository {
    override suspend fun updateSubscriptions(
        fcmToken: String,
        subscriptions: List<SubscriptionRequest>,
        locale: String,
    ) {
        onUpdateSubscriptions(fcmToken, subscriptions, locale)
    }

    override suspend fun updateAccessibility(
        fcmToken: String,
        includeAccessibility: Boolean,
        locale: String,
    ) {
        onUpdateAccessibility(fcmToken, includeAccessibility, locale)
    }
}
