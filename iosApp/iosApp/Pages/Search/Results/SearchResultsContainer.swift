//
//  SearchResultsContainer.swift
//  iosApp
//
//  Created by Simon, Emma on 1/29/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
import Shared
import SwiftUI

struct SearchResultsContainer: View {
    let query: String

    @ObservedObject var nearbyVM: NearbyViewModel
    @State var searchVM: ISearchViewModel
    @State var searchVMState: SearchViewModel.State = SearchViewModel.StateLoading.shared

    var didAppear: ((Self) -> Void)?
    var didChange: ((Self) -> Void)?

    init(
        query: String,
        nearbyVM: NearbyViewModel,
        searchVM: ISearchViewModel,
        didAppear: ((Self) -> Void)? = nil,
        didChange: ((Self) -> Void)? = nil
    ) {
        self.query = query
        self.nearbyVM = nearbyVM
        self.searchVM = searchVM
        self.didAppear = didAppear
        self.didChange = didChange
    }

    func handleStopTap(stopId: String) {
        nearbyVM.pushNavEntry(.stopDetails(stopId: stopId, stopFilter: nil, tripFilter: nil))
    }

    func handleRouteTap(routeId: String) {
        nearbyVM.pushNavEntry(.routeDetails(.init(routeId: routeId, context: .Details.shared)))
    }

    var body: some View {
        SearchResultsView(
            state: searchVMState,
            handleStopTap: handleStopTap,
            handleRouteTap: handleRouteTap,
        )
        .task {
            for await state in searchVM.models {
                searchVMState = state
            }
        }
        .onAppear {
            searchVM.setQuery(query: query)
            searchVM.refreshHistory()
            didAppear?(self)
        }
        .onChange(of: query) { query in
            searchVM.setQuery(query: query)
            didChange?(self)
        }
    }
}

struct SearchResultView_Previews: PreviewProvider {
    static var previews: some View {
        SearchResultsView(
            state: SearchViewModel.StateResults(
                stops: [
                    SearchViewModel.StopResult(
                        id: "place-haecl",
                        isStation: true,
                        name: "Haymarket",
                        routePills: [
                            RoutePillSpec(
                                textColor: "#FFFFFF",
                                routeColor: "#ED8B00",
                                content: RoutePillSpecContentText(text: "OL"),
                                size: RoutePillSpec.Size.flexPillSmall,
                                shape: RoutePillSpec.Shape.capsule,
                                contentDescription: nil
                            ),
                        ]
                    ),
                ],
                routes: [
                    SearchViewModel.RouteResult(
                        id: "428",
                        name: "Oaklandvale - Haymarket Station",
                        routePill: .init(
                            textColor: "#000000",
                            routeColor: "#FFC72C",
                            content: RoutePillSpecContentText(text: "428"),
                            size: .fixedPill,
                            shape: .rectangle,
                            contentDescription: nil
                        )
                    ),
                ]
            ),
            handleStopTap: { _ in },
            handleRouteTap: { _ in },
        ).font(Typography.body).previewDisplayName("SearchResultView")
    }
}
