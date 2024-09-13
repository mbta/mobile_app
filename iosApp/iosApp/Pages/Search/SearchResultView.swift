//
//  SearchResultView.swift
//  iosApp
//
//  Created by Simon, Emma on 1/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
import shared
import SwiftUI

class TextFieldObserver: ObservableObject {
    @Published var debouncedText = ""
    @Published var searchText = ""

    private var subscriptions = Set<AnyCancellable>()

    init() {
        $searchText
            .debounce(for: .seconds(0.5), scheduler: DispatchQueue.main)
            .sink(receiveValue: { [weak self] nextText in
                self?.debouncedText = nextText
            })
            .store(in: &subscriptions)
    }
}

struct SearchView: View {
    let query: String
    let globalRepository: IGlobalRepository
    let searchResultsRepository: ISearchResultRepository

    @State var globalResponse: GlobalResponse?
    @State var searchResults: SearchResults?

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
        didAppear: ((Self) -> Void)? = nil,
        didChange: ((Self) -> Void)? = nil
    ) {
        self.query = query
        self.nearbyVM = nearbyVM
        self.searchVM = searchVM
        self.globalRepository = globalRepository
        self.searchResultsRepository = searchResultsRepository
        self.didAppear = didAppear
        self.didChange = didChange
    }

    func loadResults(query: String) {
        Task {
            do {
                searchResults = try await searchResultsRepository.getSearchResults(query: query)
            } catch {
                debugPrint(error)
            }
        }
    }

    func handleStopTap(stopId: String) {
        guard let stop = globalResponse?.stops[stopId] else { return }
        nearbyVM.navigationStack.removeAll()
        nearbyVM.navigationStack.append(.stopDetails(stop, nil))
    }

    var body: some View {
        VStack {
            if !query.isEmpty {
                SearchResultView(
                    results: searchResults,
                    handleStopTap: handleStopTap,
                    showRoutes: searchVM.routeResultsEnabled
                )
            }
        }
        .onAppear {
            loadResults(query: query)
            Task {
                await searchVM.loadSettings()
            }
            Task {
                do {
                    globalResponse = try await globalRepository.getGlobalData()
                } catch {
                    debugPrint(error)
                }
            }
            didAppear?(self)
        }
        .onChange(of: query) { query in
            loadResults(query: query)
            didChange?(self)
        }
    }
}

struct SearchResultView: View {
    private var results: SearchResults?
    private var handleStopTap: (String) -> Void
    private var showRoutes: Bool
    init(
        results: SearchResults? = nil,
        handleStopTap: @escaping (String) -> Void,
        showRoutes: Bool = false
    ) {
        self.results = results
        self.handleStopTap = handleStopTap
        self.showRoutes = showRoutes
    }

    var body: some View {
        if results == nil {
            Text("Loading...")
        } else {
            if results!.stops.isEmpty, !showRoutes || results!.routes.isEmpty {
                Text("No results found")
            } else {
                List {
                    if !results!.stops.isEmpty {
                        Section(header: Text("Stops")) {
                            ForEach(results!.stops, id: \.id) { stop in
                                StopResultView(stop: stop)
                                    .onTapGesture { handleStopTap(stop.id) }
                            }
                        }
                    }
                    if showRoutes, !results!.routes.isEmpty {
                        Section(header: Text("Routes")) {
                            ForEach(results!.routes, id: \.id) {
                                RouteResultView(route: $0)
                            }
                        }
                    }
                }
            }
        }
    }
}

struct StopResultView: View {
    let stop: StopResult

    var body: some View {
        VStack(alignment: .leading) {
            Text(stop.name)
        }
    }
}

struct RouteResultView: View {
    let route: RouteResult

    var body: some View {
        VStack(alignment: .leading) {
            if route.routeType == RouteType.bus {
                Text(verbatim: "\(route.shortName) \(route.longName)")
            } else {
                Text(route.longName)
            }
        }
    }
}

struct SearchResultView_Previews: PreviewProvider {
    static var previews: some View {
        SearchResultView(
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
            handleStopTap: { _ in }
        ).font(Typography.body).previewDisplayName("SearchResultView")
    }
}
