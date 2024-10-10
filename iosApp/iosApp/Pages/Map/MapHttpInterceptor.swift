//
//  MapHttpInterceptor.swift
//  iosApp
//
//  Created by Brady, Kayla on 7/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import MapboxMaps

class MapHttpInterceptor: HttpServiceInterceptorInterface {
    var updateLastErrorTimestamp: () -> Void

    init(updateLastErrorTimestamp: @escaping () -> Void) {
        self.updateLastErrorTimestamp = updateLastErrorTimestamp
    }

    func onRequest(for request: HttpRequest, continuation: @escaping HttpServiceInterceptorRequestContinuation) {
        // no-op
        continuation(HttpRequestOrResponse.fromHttpRequest(request))
    }

    func onResponse(for response: HttpResponse, continuation: @escaping HttpServiceInterceptorResponseContinuation) {
        switch response.result {
        case let .success(data):
            // 401 happens here
            if data.code == 401 {
                updateLastErrorTimestamp()
            }
        case let .failure(error):
            break
        }
        continuation(response)
    }
}
