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
        case recentStops(stops: [Result])
        case results(stops: [Result], routes: [RouteResult], includeRoutes: Bool)
        case empty
        case error
    }

    struct Result: Identifiable, Equatable {
        let id: String
        let isStation: Bool
        let name: String
        let routePills: [RoutePillSpec]
    }

    let query: String
    let globalRepository: IGlobalRepository
    let searchResultsRepository: ISearchResultRepository
    let visitHistoryUsecase: VisitHistoryUsecase

    @State var globalResponse: GlobalResponse?
    @State var latestVisits: [Result]?
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

    func loadResults(query: String) async {
        do {
            resultsState = .loading
            switch try await onEnum(of: searchResultsRepository.getSearchResults(query: query)) {
            case let .ok(result):
                let showRoutes = searchVM.routeResultsEnabled
                let results = result.data
                resultsState = if results.stops.isEmpty, !showRoutes || results.routes.isEmpty {
                    .empty
                } else {
                    .results(
                        stops: results.stops.compactMap { mapStopIdToResult(id: $0.id) },
                        routes: results.routes,
                        includeRoutes: showRoutes
                    )
                }
            case nil: resultsState = .error
            case let .error(error):
                resultsState = .error
                debugPrint(error)
            }
        } catch {}
    }

    func loadVisitHistory() async {
        do {
            latestVisits = try await visitHistoryUsecase.getLatestVisits()
                .compactMap { visit in
                    switch onEnum(of: visit) {
                    case let .stopVisit(stopVisit):
                        mapStopIdToResult(id: stopVisit.stopId)
                    }
                }
        } catch {}
    }

    private func mapStopIdToResult(id: String) -> Result? {
        guard let globalResponse, let stop = globalResponse.stops[id] else { return nil }
        let isStation = stop.locationType == .station
        let routePills: [RoutePillSpec] = globalResponse.trips
            .filter { trip -> Bool in
                let routePattern: RoutePattern? = if let routePatternId = trip.value.routePatternId {
                    globalResponse.routePatterns[routePatternId]
                } else { nil }
                return trip.value.stopIds?.contains(stop.id) == true ||
                    trip.value.stopIds?.contains(where: stop.childStopIds.contains) == true &&
                    (routePattern?.typicality == .typical)
            }
            .compactMap { trip -> Route? in return globalResponse.routes[trip.value.routeId] }
            .sorted(by: { $0.sortOrder < $1.sortOrder })
            .map { route -> RoutePillSpec in
                let line: Line? = if let lineId = route.lineId { globalResponse.lines[lineId] } else { nil }
                let context: RoutePillSpec.Context = isStation ? .searchStation : .default
                return RoutePillSpec(route: route, line: line, type: .flexCompact, context: context)
            }

        return Result(
            id: stop.id,
            isStation: isStation,
            name: stop.name,
            routePills: routePills.removingDuplicates()
        )
    }

    func handleStopTap(stopId: String) {
        guard let stop = globalResponse?.stops[stopId] else { return }
        nearbyVM.navigationStack.append(.stopDetails(stop, nil))
    }

    func showRecentStops() {
        guard let latestVisits, !latestVisits.isEmpty else { return }
        resultsState = .recentStops(stops: latestVisits)
    }

    var body: some View {
        SearchResultsView(
            state: resultsState,
            handleStopTap: handleStopTap
        )
        .onAppear {
            if !query.isEmpty {
                Task { await loadResults(query: query) }
            }
            Task { await loadVisitHistory() }
            Task { await searchVM.loadSettings() }
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
                Task { await loadResults(query: query) }
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
                stops: [
                    SearchResultsContainer.Result(
                        id: "place-haecl",
                        isStation: true,
                        name: "Haymarket",
                        routePills: [
                            RoutePillSpec(
                                textColor: "#FFFFFF",
                                routeColor: "#ED8B00",
                                content: RoutePillSpecContentText(text: "OL"),
                                size: RoutePillSpec.Size.flexPillSmall,
                                shape: RoutePillSpec.Shape.capsule
                            ),
                        ]
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
                ],
                includeRoutes: true
            ),
            handleStopTap: { _ in }
        ).font(Typography.body).previewDisplayName("SearchResultView")
    }
}
