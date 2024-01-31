//
//  SearchResultView.swift
//  iosApp
//
//  Created by Simon, Emma on 1/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Combine
import SwiftUI
import shared

class TextFieldObserver : ObservableObject {
    @Published var debouncedText = ""
    @Published var searchText = ""
    
    private var subscriptions = Set<AnyCancellable>()
    
    init() {
        $searchText
            .debounce(for: .seconds(0.5), scheduler: DispatchQueue.main)
            .sink(receiveValue: { [weak self] t in
                self?.debouncedText = t
            } )
            .store(in: &subscriptions)
    }
}

struct SearchResultView: View {
    @ObservedObject private(set) var viewModel: ViewModel

    var body: some View {
        if let response = viewModel.response {
            if (response.data.stops.isEmpty && response.data.routes.isEmpty) {
                Text("No results found")
            } else {
                List {
                    if (!response.data.stops.isEmpty) {
                        Section(header: Text("Stops")) {
                            ForEach(response.data.stops, id: \.id) {
                                StopResultView(stop: $0)
                            }
                        }
                    }
                    if (!response.data.routes.isEmpty) {
                        Section(header: Text("Routes")) {
                            ForEach(response.data.routes, id: \.id) {
                                RouteResultView(route: $0)
                            }
                        }
                    }
                }
            }
        } else {
            Text("Loading...")
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
            if (route.routeType == RouteType.bus) {
                Text("\(route.shortName) \(route.longName)")
            } else {
                Text(route.longName)
            }
        }
    }
}

extension SearchResultView {
    @MainActor class ViewModel: ObservableObject {
        let backend: BackendDispatcher
        @Published var response: SearchResponse? = nil
        @Published var query: String? = nil

        init(query: String?, backend: BackendDispatcher, response: SearchResponse? = nil) {
            self.query = query
            self.backend = backend
            self.response = response
            getResults()
        }

        func getResults() {
            Task {
                guard let query = self.query else { return }
                do {
                    response = try await backend.getSearchResults(query: query)
                } catch let error {
                    debugPrint(error)
                }
            }
        }
    }
}

struct SearchResultView_Previews: PreviewProvider {
    static var previews: some View {
        SearchResultView(viewModel: .init(
            query: "Hay",
            backend: BackendDispatcher(backend: IdleBackend()),
            response: SearchResponse(
                data: SearchResults(
                    routes: [
                        RouteResult(
                            id: "428",
                            rank: 5,
                            longName: "Oaklandvale - Haymarket Station", 
                            shortName: "428",
                            routeType: RouteType.bus
                        )
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
                                    type: RouteType.subway,
                                    icon: "orange_line"
                                )
                            ]
                        )
                    ]
                )
            )
        )).previewDisplayName("SearchResultView")
    }
}
