//
//  SearchOverlay.swift
//  iosApp
//
//  Created by esimon on 9/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct SearchOverlay: View {
    @ObservedObject var searchObserver: TextFieldObserver
    @ObservedObject var nearbyVM: NearbyViewModel
    @State var searchVM: SearchViewModel

    let globalRepository: IGlobalRepository
    let searchResultsRepository: ISearchResultRepository

    init(
        searchObserver: TextFieldObserver,
        nearbyVM: NearbyViewModel,
        searchVM: SearchViewModel = ViewModelDI().search,
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
            if searchObserver.isSearching {
                ZStack(alignment: .top) {
                    SearchResultsContainer(
                        query: searchObserver.searchText,
                        nearbyVM: nearbyVM,
                        searchVM: searchVM
                    )
                    Divider()
                        .frame(height: 2)
                        .overlay(Color.halo)
                }.padding(.top, 16)
            }
        }.background(searchObserver.isSearching ? Color.fill2 : Color.clear)
    }
}
