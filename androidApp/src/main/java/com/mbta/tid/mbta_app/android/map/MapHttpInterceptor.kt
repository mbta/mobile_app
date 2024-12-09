package com.mbta.tid.mbta_app.android.map

import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestOrResponse
import com.mapbox.common.HttpResponse
import com.mapbox.common.HttpServiceInterceptorInterface
import com.mapbox.common.HttpServiceInterceptorRequestContinuation
import com.mapbox.common.HttpServiceInterceptorResponseContinuation

class MapHttpInterceptor(val updateLastErrorTimestamp: () -> Unit) :
    HttpServiceInterceptorInterface {
    override fun onRequest(
        request: HttpRequest,
        continuation: HttpServiceInterceptorRequestContinuation
    ) {
        continuation.run(HttpRequestOrResponse(request))
    }

    override fun onResponse(
        response: HttpResponse,
        continuation: HttpServiceInterceptorResponseContinuation
    ) {

        val responseData = response.result.value

        if (responseData != null) {
            if (responseData.code == 401) {
                updateLastErrorTimestamp()
            }
        }
        continuation.run(response)
    }
}
