//
//  SearchViewModel.swift
//  iosApp
//
//  Created by esimon on 9/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import shared

class SearchViewModel: ObservableObject {
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

    @Published var resultsState: ResultsState?

    private let settingsRepo: ISettingsRepository
    private let visitHistoryUsecase: VisitHistoryUsecase
    private let searchResultsRepository: ISearchResultRepository
    private let globalRepository: IGlobalRepository

    private var routeResultsEnabled: Bool
    private var globalResponse: GlobalResponse?
    private var latestVisits: [Result]?
    private var fetchResultsTask: Task<Void, Never>?

    init(
        routeResultsEnabled: Bool = false,
        settingsRepo: ISettingsRepository = RepositoryDI().settings,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        visitHistoryUsecase: VisitHistoryUsecase = UsecaseDI().visitHistoryUsecase,
        searchResultsRepository: ISearchResultRepository = RepositoryDI().searchResults
    ) {
        self.routeResultsEnabled = routeResultsEnabled
        self.settingsRepo = settingsRepo
        self.globalRepository = globalRepository
        self.visitHistoryUsecase = visitHistoryUsecase
        self.searchResultsRepository = searchResultsRepository
    }

    func getStopFor(id: String) -> Stop? {
        globalResponse?.stops[id]
    }

    func determineStateFor(query: String) {
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
        await loadVisitHistory()
    }

    func loadSettings() async {
        do {
            let settings = try await settingsRepo.getSettings()
            await MainActor.run { [settings] in
                routeResultsEnabled = settings.first(where: { $0.key == .searchRouteResults })?.isOn ?? false
            }
        } catch {}
    }

    private func getGlobalData() async {
        do {
            switch try await onEnum(of: globalRepository.getGlobalData()) {
            case let .ok(result): globalResponse = result.data
            case let .error(error): debugPrint(error)
            }
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
                let showRoutes = routeResultsEnabled
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
              let stop = globalResponse.stops[id]
        else { return nil }
        let isStation = stop.locationType == .station
        let routes = globalResponse.getTypicalRoutesFor(stopId: id)
        let routePills: [RoutePillSpec] = routes
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
}