//
//  FetchApiTests.swift
//  iosAppTests
//
//  Created by Horn, Melody on 2024-10-02.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import XCTest

final class FetchApiTests: XCTestCase {
    class Data: Equatable {
        let value: Int

        init(_ value: Int) {
            self.value = value
        }

        static func == (lhs: FetchApiTests.Data, rhs: FetchApiTests.Data) -> Bool {
            lhs.value == rhs.value
        }
    }

    func testCallsSuccessAndClearsError() async throws {
        let errorBannerRepo = ErrorBannerStateRepository()
        errorBannerRepo.setDataError(key: "a", details: "", action: {})
        XCTAssertNotNil(errorBannerRepo.state.value)
        let expSuccess = expectation(description: "calls onSuccess")
        await fetchApi(
            errorBannerRepo,
            errorKey: "a",
            getData: { ApiResultOk(data: Data(4)) },
            onSuccess: {
                XCTAssertEqual($0, Data(4))
                expSuccess.fulfill()
            },
            onRefreshAfterError: { XCTFail("did not call onSuccess") }
        )
        await fulfillment(of: [expSuccess], timeout: 1)
        XCTAssertNil(errorBannerRepo.state.value)
    }

    func testStoresApiError() async throws {
        let errorBannerRepo = ErrorBannerStateRepository()
        XCTAssertNil(errorBannerRepo.state.value)
        let expRefresh = expectation(description: "can refresh after error")
        await fetchApi(
            errorBannerRepo,
            errorKey: "a",
            getData: { ApiResultError<Data>(code: 418, message: "I'm a teapot") },
            onSuccess: { _ in XCTFail("called onSuccess") },
            onRefreshAfterError: { expRefresh.fulfill() }
        )
        XCTAssertNotNil(errorBannerRepo.state.value)

        if let action = errorBannerRepo.state.value?.action {
            action()
        } else {
            XCTFail("data error missing action")
        }
        await fulfillment(of: [expRefresh], timeout: 1)
    }

    func testHandlesThrownError() async throws {
        struct AdHocError: Error {}
        let errorBannerRepo = ErrorBannerStateRepository()
        await fetchApi(
            errorBannerRepo,
            errorKey: "a",
            getData: { throw AdHocError() },
            onSuccess: { (_: Data) in XCTFail("called onSuccess") },
            onRefreshAfterError: { XCTFail("called onRefresh") }
        )
        XCTAssertNotNil(errorBannerRepo.state.value)
    }

    func testHandlesCancellation() async throws {
        let errorBannerRepo = ErrorBannerStateRepository()
        let expFetchStarted = expectation(description: "fetch started")
        let task = Task {
            await fetchApi(
                errorBannerRepo,
                errorKey: "a",
                getData: {
                    expFetchStarted.fulfill()
                    try await Task.sleep(for: .seconds(1))
                    XCTFail("task was supposed to be cancelled by now")
                    return ApiResultOk(data: Data(10))
                },
                onSuccess: { (_: Data) in XCTFail("called onSuccess") },
                onRefreshAfterError: { XCTFail("called onRefresh") }
            )
        }
        await fulfillment(of: [expFetchStarted], timeout: 1)
        task.cancel()
        // give the failures some time to fail
        try await Task.sleep(for: .milliseconds(200))
        XCTAssertNil(errorBannerRepo.state.value)
    }
}
