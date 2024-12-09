package com.mbta.tid.mbta_app.android.map

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpRequestErrorType
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpResponseData
import com.mapbox.common.SdkInformation
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MapHttpInterceptorTests {
    private val fakeRequest: HttpRequest =
        HttpRequest.Builder()
            .method(HttpMethod.GET)
            .url("fake_url")
            .headers(hashMapOf())
            .sdkInformation(SdkInformation("a", "b", "c"))
            .build()

    @Test
    fun testOnRequestUnchanged() = runTest {
        var requestUnchanged = false
        val interceptor = MapHttpInterceptor {}
        interceptor.onRequest(fakeRequest) { requstOrResponse ->
            if (requstOrResponse.getHttpRequest() == fakeRequest) {
                requestUnchanged = true
            }
        }

        assertTrue { requestUnchanged }
    }

    @Test
    fun testOnResponse200DoesNothing() = runTest {
        val fakeResponse: HttpResponse =
            HttpResponse(
                1,
                fakeRequest,
                ExpectedFactory.createValue(HttpResponseData(hashMapOf(), 200, byteArrayOf()))
            )

        var responseUnchanged = false
        var updateLastErrorCalled = false
        val interceptor = MapHttpInterceptor { updateLastErrorCalled = true }
        interceptor.onResponse(fakeResponse) { requstOrResponse ->
            if (requstOrResponse.request == fakeRequest) {
                responseUnchanged = true
            }
        }

        assertTrue { responseUnchanged }
        assertFalse { updateLastErrorCalled }
    }

    @Test
    fun testOn401UpdatesLastError() = runTest {
        val fakeResponse =
            HttpResponse(
                1,
                fakeRequest,
                ExpectedFactory.createValue(HttpResponseData(hashMapOf(), 401, byteArrayOf()))
            )

        var responseUnchanged = false
        var updateLastErrorCalled = false
        val interceptor = MapHttpInterceptor { updateLastErrorCalled = true }
        interceptor.onResponse(fakeResponse) { requstOrResponse ->
            if (requstOrResponse.request == fakeRequest) {
                responseUnchanged = true
            }
        }

        assertTrue { responseUnchanged }
        assertTrue { updateLastErrorCalled }
    }

    @Test
    fun testOnRequsetCancelledDoesNothing() = runTest {
        val fakeResponse =
            HttpResponse(
                1,
                fakeRequest,
                ExpectedFactory.createError(
                    HttpRequestError(HttpRequestErrorType.REQUEST_CANCELLED, "cancelled")
                )
            )

        var responseUnchanged = false
        var updateLastErrorCalled = false
        val interceptor = MapHttpInterceptor { updateLastErrorCalled = true }
        interceptor.onResponse(fakeResponse) { requstOrResponse ->
            if (requstOrResponse.request == fakeRequest) {
                responseUnchanged = true
            }
        }

        assertTrue { responseUnchanged }
        assertFalse { updateLastErrorCalled }
    }
}
