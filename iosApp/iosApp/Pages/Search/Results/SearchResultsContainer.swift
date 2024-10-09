//
//  SearchResultsContainer.swift
//  iosApp
//
//  Created by Simon, Emma on 1/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
import shared
import SwiftUI

struct SearchResultsContainer: View {
    enum ResultsState: Equatable {
        case loading
        case recentStops(stops: [StopResult])
        case results(results: SearchResults, includeRoutes: Bool)
        case empty
        case error
    }

    let query: String
    let globalRepository: IGlobalRepository
    let searchResultsRepository: ISearchResultRepository
    let visitHistoryUsecase: VisitHistoryUsecase

    @State var globalResponse: GlobalResponse?
    @State var latestVisits: [StopResult]?
    @State var resultsState: ResultsState?

    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var searchVM: SearchViewModel

    var didAppear: ((Self) -> Void)?
    var didChange: ((Self) -> Void)?

    init(
        query: String,
        nearbyVM: NearbyViewModel,
        searchVM: SearchViewModel,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        searchResultsRepository: ISearchResultRepository = RepositoryDI().searchResults,
        visitHistoryUsecase: VisitHistoryUsecase = UsecaseDI().visitHistoryUsecase,
        didAppear: ((Self) -> Void)? = nil,
        didChange: ((Self) -> Void)? = nil
    ) {
        self.query = query
        self.nearbyVM = nearbyVM
        self.searchVM = searchVM
        self.globalRepository = globalRepository
        self.searchResultsRepository = searchResultsRepository
        self.visitHistoryUsecase = visitHistoryUsecase
        self.didAppear = didAppear
        self.didChange = didChange
    }

    func loadResults(query: String) {
        Task {
            resultsState = .loading
            switch try await onEnum(of: searchResultsRepository.getSearchResults(query: query)) {
            case let .ok(result):
                let showRoutes = searchVM.routeResultsEnabled
                let results = result.data
                resultsState = if results.stops.isEmpty, !showRoutes || results.routes.isEmpty {
                    .empty
                } else {
                    .results(results: results, includeRoutes: showRoutes)
                }
            case nil: resultsState = .error
            case let .error(error):
                resultsState = .error
                debugPrint(error)
            }
        }
    }

    func loadVisitHistory() {
        Task {
            do {
                latestVisits = try await visitHistoryUsecase.getLatestVisits()
                    .compactMap { visit in
                        switch onEnum(of: visit) {
                        case let .stopVisit(stopVisit):
                            // TODO: https://app.asana.com/0/1205425564113216/1208355161933391/f
                            globalResponse?
                                .stops[stopVisit.stopId]
                                .map { stop in
                                    StopResult(
                                        id: stop.id,
                                        rank: 0,
                                        name: stop.name,
                                        zone: nil,
                                        isStation: true,
                                        routes: []
                                    )
                                }
                        }
                    }
            } catch {}
        }
    }

    func handleStopTap(stopId: String) {
        guard let stop = globalResponse?.stops[stopId] else { return }
        nearbyVM.navigationStack.append(.stopDetails(stop, nil))
    }

    func showRecentStops() {
        resultsState = nil
        // TODO: https://app.asana.com/0/1205425564113216/1208355161933391/f
        // guard let latestVisits, !latestVisits.isEmpty else { return }
        // resultsState = .recentStops(stops: latestVisits)
    }

    var body: some View {
        SearchResultsView(
            state: resultsState,
            handleStopTap: handleStopTap
        )
        .onAppear {
            if !query.isEmpty {
                loadResults(query: query)
            }
            loadVisitHistory()
            Task {
                await searchVM.loadSettings()
            }
            Task {
                switch try await onEnum(of: globalRepository.getGlobalData()) {
                case let .ok(result): globalResponse = result.data
                case let .error(error): debugPrint(error)
                }
            }
            didAppear?(self)
        }
        .onChange(of: query) { query in
            if query.isEmpty {
                showRecentStops()
            } else {
                loadResults(query: query)
            }
            didChange?(self)
        }
        .onChange(of: latestVisits) { _ in
            if query.isEmpty {
                showRecentStops()
            }
        }
    }
}

struct SearchResultView_Previews: PreviewProvider {
    static var previews: some View {
        SearchResultsView(
            state: .results(
                results: SearchResults(
                    routes: [
                        RouteResult(
                            id: "428",
                            rank: 5,
                            longName: "Oaklandvale - Haymarket Station",
                            shortName: "428",
                            routeType: RouteType.bus
                        ),
                    ],
                    stops: [
                        StopResult(
                            id: "place-haecl",
                            rank: 2,
                            name: "Haymarket",
                            zone: nil,
                            isStation: true,
                            routes: [
                                StopResultRoute(
                                    type: RouteType.heavyRail,
                                    icon: "orange_line"
                                ),
                            ]
                        ),
                    ]
                ),
                includeRoutes: true
            ),
            handleStopTap: { _ in }
        ).font(Typography.body).previewDisplayName("SearchResultView")
    }
}
