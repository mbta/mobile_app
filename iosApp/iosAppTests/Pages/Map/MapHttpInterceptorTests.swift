//
//  MapHttpInterceptorTests.swift
//  iosAppTests
//
//  Created by Brady, Kayla on 7/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import XCTest
@_spi(Experimental) import MapboxMaps

final class MapHttpInterceptorTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    let fakeRequest: HttpRequest = .init(url: "fake_url",
                                         headers: [:],
                                         sdkInformation: .init(name: "name", version: "v", packageName: nil),
                                         body: nil)

    func testOnRequest() throws {
        var requestUnchanged = false
        let interceptor = MapHttpInterceptor(updateLastErrorTimestamp: {})
        interceptor.onRequest(for: fakeRequest) { requstOrResponse in
            if requstOrResponse.getHttpRequest() == self.fakeRequest {
                requestUnchanged = true
            }
        }

        XCTAssertTrue(requestUnchanged)
    }

    func testOnResponse200DoesNothing() throws {
        let fake200Response: HttpResponse = .init(identifier: 1, request: fakeRequest,
                                                  result: .success(.init(headers: [:], code: 200, data: .init())))
        var responseUnchanged = false
        var updateLastErrorCalled = false

        let interceptor = MapHttpInterceptor(updateLastErrorTimestamp: { updateLastErrorCalled = true })
        interceptor.onResponse(for: fake200Response, continuation: { response in
            if response == fake200Response {
                responseUnchanged = true
            }
        })

        XCTAssertTrue(responseUnchanged)
        XCTAssertFalse(updateLastErrorCalled)
    }

    func testOnResponse401UpdatesLastError() throws {
        let fake401Response: HttpResponse = .init(identifier: 1, request: fakeRequest,
                                                  result: .success(.init(headers: [:], code: 401, data: .init())))
        var responseUnchanged = false
        var updateLastErrorCalled = false

        let interceptor = MapHttpInterceptor(updateLastErrorTimestamp: { updateLastErrorCalled = true })
        interceptor.onResponse(for: fake401Response, continuation: { response in
            if response == fake401Response {
                responseUnchanged = true
            }
        })

        XCTAssertTrue(responseUnchanged)
        XCTAssertTrue(updateLastErrorCalled)
    }

    func testOnRequestCancelledDoesNothing() throws {
        let fakeErrorResponse: HttpResponse = .init(identifier: 1, request: fakeRequest,
                                                    result: .failure(.init(type: .requestCancelled,
                                                                           message: "Cancelled")))
        var responseUnchanged = false
        var updateLastErrorCalled = false

        let interceptor = MapHttpInterceptor(updateLastErrorTimestamp: { updateLastErrorCalled = true })
        interceptor.onResponse(for: fakeErrorResponse, continuation: { response in
            if response == fakeErrorResponse {
                responseUnchanged = true
            }
        })

        XCTAssertTrue(responseUnchanged)
        XCTAssertFalse(updateLastErrorCalled)
    }
}
