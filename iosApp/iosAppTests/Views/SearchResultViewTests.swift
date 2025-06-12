//
//  SearchResultViewTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 1/30/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class SearchResultViewTests: XCTestCase {
    override func setUp() {
        executionTimeAllowance = 60
    }

    @MainActor func testPending() throws {
        let sut = SearchResultsView(state: .loading, handleStopTap: { _ in }).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().view(SearchResultsView.self).find(LoadingResults.self))
    }

    @MainActor func testResultLoad() throws {
        class FakeRepo: ISearchResultRepository {
            let getSearchResultsExpectation: XCTestExpectation

            init(getSearchResultsExpectation: XCTestExpectation) {
                self.getSearchResultsExpectation = getSearchResultsExpectation
            }

            func __getSearchResults(query _: String) async throws -> ApiResult<SearchResults>? {
                getSearchResultsExpectation.fulfill()
                return nil
            }
        }

        let getSearchResultsExpectation = expectation(description: "getSearchResults")

        var sut = SearchResultsContainer(
            query: "hay",
            nearbyVM: NearbyViewModel(),
            searchVM: SearchViewModel(
                searchResultsRepository: FakeRepo(getSearchResultsExpectation: getSearchResultsExpectation)
            )
        )

        let hasAppeared = sut.on(\.didAppear) { _ in }
        ViewHosting.host(view: sut)

        wait(for: [hasAppeared], timeout: 1)
        wait(for: [getSearchResultsExpectation], timeout: 1)
    }

    @MainActor func testOverlayDisplayedOnFocus() throws {
        class FakeRepo: ISearchResultRepository {
            let getSearchResultsExpectation: XCTestExpectation

            init(getSearchResultsExpectation: XCTestExpectation) {
                self.getSearchResultsExpectation = getSearchResultsExpectation
            }

            func __getSearchResults(query _: String) async throws -> ApiResult<SearchResults>? {
                getSearchResultsExpectation.fulfill()
                return ApiResultOk(data: SearchResults(routes: [], stops: []))
            }
        }

        let getSearchResultsExpectation = expectation(description: "getSearchResults")
        getSearchResultsExpectation.assertForOverFulfill = false
        let searchObserver = TextFieldObserver()

        let sut = SearchOverlay(
            searchObserver: searchObserver,
            nearbyVM: NearbyViewModel(),
            searchVM: SearchViewModel(
                searchResultsRepository: FakeRepo(getSearchResultsExpectation: getSearchResultsExpectation)
            )
        )

        ViewHosting.host(view: sut.withFixedSettings([:]))

        // On init, only the search field should be displayed
        XCTAssertNotNil(try sut.inspect().find(SearchField.self))
        XCTAssertThrowsError(try sut.inspect().find(SearchResultsView.self))
        XCTAssertThrowsError(try sut.inspect().find(SearchField.self).find(button: "Cancel"))
        // On focus, the result view and cancel button should appear,
        // but the clear button should be hidden
        searchObserver.isFocused = true
        XCTAssertNotNil(try sut.inspect().find(SearchResultsView.self))
        XCTAssertNotNil(try sut.inspect().find(SearchField.self).find(button: "Cancel"))
        XCTAssertThrowsError(try sut.inspect().find(SearchField.self).find(ActionButton.self))
        XCTAssert(searchObserver.isSearching)
        // Once text is entered, the search repo should be called
        searchObserver.searchText = "test"
        wait(for: [getSearchResultsExpectation], timeout: 1)
        XCTAssertEqual("test", searchObserver.searchText)
        // Even if the focus is then lost, the result view should still be displayed
        searchObserver.isFocused = false
        XCTAssertNotNil(try sut.inspect().find(SearchResultsView.self))
        // When the search field is cleared, the field should be refocused
        try sut.inspect().find(SearchField.self).find(ActionButton.self).implicitAnyView().button().tap()
        XCTAssertThrowsError(try sut.inspect().find(SearchField.self).find(ActionButton.self))
        XCTAssert(searchObserver.isSearching)
        XCTAssertNotNil(try sut.inspect().find(SearchResultsView.self))
        // When cancel is tapped, the results should disappear and the search field be unfocused
        try sut.inspect().find(SearchField.self).find(button: "Cancel").tap()
        XCTAssertThrowsError(try sut.inspect().find(SearchResultsView.self))
        XCTAssertFalse(searchObserver.isSearching)
    }

    @MainActor func testNoResults() throws {
        let sut = SearchResultsView(state: .results(stops: [], routes: []), handleStopTap: { _ in })
            .withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().view(SearchResultsView.self).find(text: "No results found 🤔"))
        XCTAssertNotNil(try sut.inspect().view(SearchResultsView.self).find(text: "Try a different spelling or name."))
    }

    @MainActor func testError() throws {
        let sut = SearchResultsView(state: .error, handleStopTap: { _ in }).withFixedSettings([:])
        XCTAssertNotNil(try sut.inspect().view(SearchResultsView.self).find(text: "Results failed to load ☹️"))
        XCTAssertNotNil(try sut.inspect().view(SearchResultsView.self).find(text: "Try your search again."))
    }

    @MainActor func testRecentStops() throws {
        let sut = SearchResultsView(
            state: .recentStops(
                stops: [
                    SearchViewModel.Result(
                        id: "place-haecl",
                        isStation: true,
                        name: "Haymarket",
                        routePills: []
                    ),
                ]
            ),
            handleStopTap: { _ in }
        ).withFixedSettings([:])

        XCTAssertNoThrow(try sut.inspect().find(text: "Haymarket"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Recently Viewed"))
    }

    @MainActor func testFullResults() throws {
        let sut = SearchResultsView(
            state: .results(
                stops: [
                    SearchViewModel.Result(
                        id: "place-haecl",
                        isStation: true,
                        name: "Haymarket",
                        routePills: []
                    ),
                ],
                routes: [
                    RouteResult(
                        id: "428",
                        rank: 5,
                        longName: "Oaklandvale - Haymarket Station",
                        shortName: "428",
                        routeType: RouteType.bus
                    ),
                ]
            ),
            handleStopTap: { _ in }
        )

        ViewHosting.host(view: sut.withFixedSettings([.searchRouteResults: true]))

        XCTAssertNoThrow(try sut.inspect().find(text: "Haymarket"))
        XCTAssertNoThrow(try sut.inspect().find(text: "Routes"))
        XCTAssertNoThrow(try sut.inspect().find(text: "428 Oaklandvale - Haymarket Station"))
    }

    @MainActor func testOnlyRoutes() throws {
        let sut = SearchResultsView(
            state: .results(
                stops: [],
                routes: [
                    RouteResult(
                        id: "428",
                        rank: 5,
                        longName: "Oaklandvale - Haymarket Station",
                        shortName: "428",
                        routeType: RouteType.bus
                    ),
                ]
            ),
            handleStopTap: { _ in }
        )

        ViewHosting.host(view: sut.withFixedSettings([.searchRouteResults: true]))

        XCTAssertNoThrow(try sut.inspect().find(text: "Routes"))
        XCTAssertNoThrow(try sut.inspect().find(text: "428 Oaklandvale - Haymarket Station"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Stops"))
    }

    @MainActor func testRoutesHidden() throws {
        let sut = SearchResultsView(
            state: .results(
                stops: [],
                routes: [
                    RouteResult(
                        id: "428",
                        rank: 5,
                        longName: "Oaklandvale - Haymarket Station",
                        shortName: "428",
                        routeType: RouteType.bus
                    ),
                ]
            ),
            handleStopTap: { _ in }
        )

        XCTAssertThrowsError(
            try sut.inspect().view(SearchResultsView.self).find(text: "Oaklandvale - Haymarket Station")
        )
    }

    @MainActor func testOnlyStops() throws {
        let sut = SearchResultsView(
            state: .results(
                stops: [
                    SearchViewModel.Result(
                        id: "place-haecl",
                        isStation: true,
                        name: "Haymarket",
                        routePills: []
                    ),
                ],
                routes: []
            ),
            handleStopTap: { _ in }
        ).withFixedSettings([:])

        XCTAssertNoThrow(try sut.inspect().find(text: "Haymarket"))
        XCTAssertThrowsError(try sut.inspect().find(text: "Routes"))
    }

    @MainActor func testStopTapping() async throws {
        let tapStopExpectation = expectation(description: "stop was tapped")

        let sut = SearchResultsView(
            state: .results(
                stops: [
                    SearchViewModel.Result(
                        id: "place-haecl",
                        isStation: true,
                        name: "Haymarket",
                        routePills: []
                    ),
                ],
                routes: []
            ),
            handleStopTap: { stopId in
                XCTAssertEqual("place-haecl", stopId)
                tapStopExpectation.fulfill()
            }
        ).withFixedSettings([:])

        XCTAssertNoThrow(
            try sut.inspect()
                .find(button: "Haymarket")
                .tap()
        )
        await fulfillment(of: [tapStopExpectation], timeout: 1)
    }
}
