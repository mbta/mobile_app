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
    let searchResultsRepository: ISearchResultRepository
    @State var searchResults: SearchResults?

    var didAppear: ((Self) -> Void)?
    var didChange: ((Self) -> Void)?

    init(
        query: String,
        searchResultsRepository: ISearchResultRepository = RepositoryDI().searchResults,
        didAppear: ((Self) -> Void)? = nil,
        didChange: ((Self) -> Void)? = nil
    ) {
        self.query = query
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

    var body: some View {
        VStack {
            if !query.isEmpty {
                SearchResultView(results: searchResults)
            }
        }
        .onAppear {
            loadResults(query: query)
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
    init(results: SearchResults? = nil) {
        self.results = results
    }

    var body: some View {
        if results == nil {
            Text("Loading...")
        } else {
            if results!.stops.isEmpty, results!.routes.isEmpty {
                Text("No results found")
            } else {
                List {
                    if !results!.stops.isEmpty {
                        Section(header: Text("Stops")) {
                            ForEach(results!.stops, id: \.id) {
                                StopResultView(stop: $0)
                            }
                        }
                    }
                    if !results!.routes.isEmpty {
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
            )
        ).font(Typography.body).previewDisplayName("SearchResultView")
    }
}
