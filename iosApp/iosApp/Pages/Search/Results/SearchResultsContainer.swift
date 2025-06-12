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
    @ObservedObject var searchVM: SearchViewModel

    var didAppear: ((Self) -> Void)?
    var didChange: ((Self) -> Void)?

    init(
        query: String,
        nearbyVM: NearbyViewModel,
        searchVM: SearchViewModel,
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
        guard let stop = searchVM.getStopFor(id: stopId) else { return }
        nearbyVM.pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil))
    }

    var body: some View {
        SearchResultsView(
            state: searchVM.resultsState,
            handleStopTap: handleStopTap
        )
        .task { await searchVM.loadGlobalDataAndHistory() }
        .onAppear {
            searchVM.determineStateFor(query: query)
            didAppear?(self)
        }
        .onChange(of: query) { query in
            searchVM.determineStateFor(query: query)
            didChange?(self)
        }
        .onDisappear {
            searchVM.resultsState = nil
        }
    }
}

struct SearchResultView_Previews: PreviewProvider {
    static var previews: some View {
        SearchResultsView(
            state: .results(
                stops: [
                    SearchViewModel.Result(
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
                                contentDescription: "Orange Line"
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
                ]
            ),
            handleStopTap: { _ in }
        ).font(Typography.body).previewDisplayName("SearchResultView")
    }
}
