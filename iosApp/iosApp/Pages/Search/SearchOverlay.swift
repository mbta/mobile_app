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
    @ObserveInjection var inject
    @ObservedObject var searchObserver: TextFieldObserver
    @State var searchVM: SearchViewModel
    @ObservedObject var navManager: NavigationManager

    let globalRepository: IGlobalRepository
    let searchResultsRepository: ISearchResultRepository

    init(
        searchObserver: TextFieldObserver,
        searchVM: SearchViewModel = ViewModelDI().search,
        globalRepository: IGlobalRepository = RepositoryDI().global,
        searchResultsRepository: ISearchResultRepository = RepositoryDI().searchResults,
        navManager: NavigationManager,
    ) {
        self.searchObserver = searchObserver
        self.searchVM = searchVM
        self.globalRepository = globalRepository
        self.searchResultsRepository = searchResultsRepository
        self.navManager = navManager
    }

    var body: some View {
        VStack(spacing: .zero) {
            SearchField(searchObserver: searchObserver)
            if searchObserver.isSearching {
                ZStack(alignment: .top) {
                    SearchResultsContainer(
                        query: searchObserver.searchText,
                        searchVM: searchVM,
                        navManager: navManager,
                    )
                    Divider()
                        .frame(height: 2)
                        .overlay(Color.halo)
                }.padding(.top, 16)
            }
        }.background(searchObserver.isSearching ? Color.fill2 : Color.clear)
            .enableInjection()
    }
}
