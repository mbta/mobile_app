//
//  SearchOverlay.swift
//  iosApp
//
//  Created by esimon on 9/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct SearchOverlay: View {
    @ObservedObject var searchObserver: TextFieldObserver
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var searchVM: SearchViewModel

    let globalRepository: IGlobalRepository
    let searchResultsRepository: ISearchResultRepository

    init(
        searchObserver: TextFieldObserver,
        nearbyVM: NearbyViewModel,
        searchVM: SearchViewModel,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        searchResultsRepository: ISearchResultRepository = RepositoryDI().searchResults
    ) {
        self.searchObserver = searchObserver
        self.nearbyVM = nearbyVM
        self.searchVM = searchVM
        self.globalRepository = globalRepository
        self.searchResultsRepository = searchResultsRepository
    }

    var body: some View {
        VStack(spacing: .zero) {
            SearchField(searchObserver: searchObserver)
                .padding(.bottom, 12)
                .background(searchObserver.isSearching ? Color.fill2 : Color.clear)
            if searchObserver.isSearching {
                ZStack(alignment: .top) {
                    SearchResultsContainer(
                        query: searchObserver.debouncedText,
                        nearbyVM: nearbyVM,
                        searchVM: searchVM,
                        globalRepository: globalRepository,
                        searchResultsRepository: searchResultsRepository
                    )
                    Divider()
                        .frame(height: 2)
                        .overlay(Color.halo)
                }
            }
        }
    }
}
