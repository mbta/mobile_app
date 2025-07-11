//
//  EditFavoritesPage.swift
//  iosApp
//
//  Created by Kayla Brady on 7/10/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct EditFavoritesPage: View {
    let viewModel: IFavoritesViewModel
    let onClose: () -> Void

    @State var globalResponse: GlobalResponse?
<<<<<<< HEAD
=======
    @State var favoritesVMState: FavoritesViewModel.State = .init()

    @State var favoritesState: [RouteStopDirection: Bool] = [:]

    @State var routeCardData: [RouteCardData]?
>>>>>>> 56bee737e (wip(ios.EditFavoritesPage): Add the requisite data)

    let errorBannerVM: ErrorBannerViewModel
    let globalRepository: IGlobalRepository = RepositoryDI().global

    var body: some View {
        VStack {
            SheetHeader(
                title: NSLocalizedString("Edit Favorites", comment: "Title for flow to edit favorites"),
                onClose: {
                    viewModel.updateFavorites(updatedFavorites: favoritesState.mapValues { KotlinBoolean(bool: $0) })
                    onClose()
                },
                closeText: NSLocalizedString("Done", comment: "Button text for closing flow")
            )
            EditFavoritesList(routeCardData: favoritesVMState.staticRouteCardData,
                              global: globalResponse, deleteFavorite: { rsd in
                                  favoritesState[rsd] = false
                              })
            Spacer()
        }
        .background(Color.fill2)
        .onAppear {
            loadGlobal()
            routeCardData = favoritesVMState.staticRouteCardData
        }
        .task {
            for await model in viewModel.models {
                favoritesVMState = model
                let initialFavorites = model.favorites ?? []
                favoritesState = initialFavorites.reduce(into: [RouteStopDirection: Bool]()) { partialResult, rsd in
                    partialResult[rsd] = true
                }
            }
        }
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            globalResponse = globalData
        }
    }

    private func loadGlobal() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "EditDetailsPage.loadGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadGlobal
            )
        }
    }
}

struct EditFavoritesList: View {
    let routeCardData: [RouteCardData]?
    let global: GlobalResponse?
    let deleteFavorite: (RouteStopDirection) -> Void

    var body: some View {
        if let routeCardData, !routeCardData.isEmpty {
            Text("TODO: show favorites here")
        }

        else if routeCardData != nil {
            ScrollView {
                // TODO: Separate NoStopsView
                Text("No stops added", comment: "Indicates the absence of favorites")
            }
        }

        else {
            ScrollView {
                LazyVStack(alignment: .center, spacing: 14) {
                    ForEach(0 ..< 5) { _ in
                        LoadingRouteCard()
                    }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 16)
                .loadingPlaceholder()
            }
        }
    }
}
