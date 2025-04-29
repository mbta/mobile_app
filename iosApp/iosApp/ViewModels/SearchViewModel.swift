//
//  SearchViewModel.swift
//  iosApp
//
//  Created by esimon on 9/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import Shared

class SearchViewModel: ObservableObject {
    enum ResultsState: Equatable {
        case loading
        case recentStops(stops: [Result])
        case results(stops: [Result], routes: [RouteResult])
        case error

        func isEmpty(includeRoutes: Bool) -> Bool {
            switch self {
            case .loading: false
            case .recentStops: false
            case let .results(stops: stops, routes: routes): stops.isEmpty && (!includeRoutes || routes.isEmpty)
            case .error: false
            }
        }
    }

    struct Result: Identifiable, Equatable {
        let id: String
        let isStation: Bool
        let name: String
        let routePills: [RoutePillSpec]
    }

    @Published var resultsState: ResultsState?

    private let visitHistoryUsecase: VisitHistoryUsecase
    private let searchResultsRepository: ISearchResultRepository
    private let globalRepository: IGlobalRepository

    private let analytics: Analytics

    private var globalResponse: GlobalResponse?
    private var latestVisits: [Result]?
    private var fetchResultsTask: Task<Void, Never>?

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        visitHistoryUsecase: VisitHistoryUsecase = UsecaseDI().visitHistoryUsecase,
        searchResultsRepository: ISearchResultRepository = RepositoryDI().searchResults,
        analytics: Analytics = AnalyticsProvider.shared
    ) {
        self.globalRepository = globalRepository
        self.visitHistoryUsecase = visitHistoryUsecase
        self.searchResultsRepository = searchResultsRepository
        self.analytics = analytics
    }

    func getStopFor(id: String) -> Stop? {
        globalResponse?.getStop(stopId: id)
    }

    func determineStateFor(query: String) {
        analytics.performedSearch(query: query)
        if query.isEmpty {
            fetchResultsTask?.cancel()
            resultsState = nil
            showRecentStops()
        } else {
            fetchResultsTask?.cancel()
            fetchResultsTask = Task { await loadResults(query: query) }
        }
    }

    func loadGlobalDataAndHistory() async {
        await getGlobalData()
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            globalResponse = globalData
            Task {
                await loadVisitHistory()
            }
        }
    }

    private func getGlobalData() async {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        do {
            let result = try await onEnum(of: globalRepository.getGlobalData())
            if case let .error(error) = result { debugPrint(error) }
        } catch {}
    }

    private func loadVisitHistory() async {
        do {
            let latestVisits = try await visitHistoryUsecase.getLatestVisits()
                .compactMap { visit in
                    switch onEnum(of: visit) {
                    case let .stopVisit(stopVisit):
                        mapStopIdToResult(id: stopVisit.stopId)
                    }
                }
            await MainActor.run { [latestVisits] in
                self.latestVisits = latestVisits
                if resultsState == nil {
                    showRecentStops()
                }
            }
        } catch {}
    }

    private func loadResults(query: String) async {
        do {
            var resultsState: ResultsState? = nil
            resultsState = .loading
            switch try await onEnum(of: searchResultsRepository.getSearchResults(query: query)) {
            case let .ok(result):
                let results = result.data
                resultsState = .results(
                    stops: results.stops.compactMap { mapStopIdToResult(id: $0.id) },
                    routes: results.routes
                )
            case nil: resultsState = .error
            case let .error(error):
                resultsState = .error
                debugPrint(error)
            }
            guard !Task.isCancelled else { return }
            await MainActor.run { [resultsState] in
                self.resultsState = resultsState
            }
        } catch {}
    }

    private func showRecentStops() {
        guard let latestVisits, !latestVisits.isEmpty else { return }
        resultsState = .recentStops(stops: latestVisits)
    }

    private func mapStopIdToResult(id: String) -> Result? {
        guard let globalResponse,
              let stop = globalResponse.getStop(stopId: id)
        else { return nil }
        let isStation = stop.locationType == .station
        let routes = globalResponse.getTypicalRoutesFor(stopId: id)
        let routePills: [RoutePillSpec] = routes
            .sorted(by: { $0.sortOrder < $1.sortOrder })
            .map { route -> RoutePillSpec in
                let line: Line? = globalResponse.getLine(lineId: route.lineId)
                let context: RoutePillSpec.Context = isStation ? .searchStation : .default
                return RoutePillSpec(route: route,
                                     line: line,
                                     type: .flexCompact,
                                     context: context,
                                     contentDescription: stopRouteContentDescription(
                                         isStation: isStation,
                                         route: route
                                     ))
            }

        return Result(
            id: stop.id,
            isStation: isStation,
            name: stop.name,
            routePills: routePills.removingDuplicates()
        )
    }

    private func stopRouteContentDescription(isStation: Bool, route: Route) -> String {
        if silverRoutes.contains(route.id), isStation {
            let routeName = "Silver Line"
            return "\(routeName) \(route.type.typeText(isOnly: false))"

        } else if route.type == .commuterRail, isStation {
            let routeName = "Commuter Rail"
            return "\(routeName) \(route.type.typeText(isOnly: false))"
        } else if route.type == .bus, isStation {
            return route.type.typeText(isOnly: false)
        } else {
            return "\(route.label) \(route.type.typeText(isOnly: true))"
        }
    }
}
