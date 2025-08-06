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
    @State var favoritesVMState: FavoritesViewModel.State = .init()
    @State var favoritesState: [RouteStopDirection: Bool] = [:]

    let errorBannerVM: ErrorBannerViewModel
    let toastVM: IToastViewModel
    let globalRepository: IGlobalRepository = RepositoryDI().global

    let inspection = Inspection<Self>()

    func deleteAndToast(_ rsd: RouteStopDirection) {
        viewModel.updateFavorites(
            updatedFavorites: [rsd: false],
            context: .favorites,
            defaultDirection: rsd.direction
        )

        let labels = rsd.getLabels(globalResponse)
        let toastMessage = if let labels {
            String(format: NSLocalizedString(
                "**%1$@ %2$@** at **%3$@** removed from Favorites",
                comment: """
                Favorite removed toast text, the first value is the direction (southbound, inbound, etc),
                the second is the route name (Red Line, 1 bus), and the third is a stop name (Ruggles, Alewife).
                The asterisks surround bolded text. ex. \"[Outbound] [71 bus] at [Harvard] removed from Favorites\"
                """
            ), labels.direction, labels.route, labels.stop,)
        } else {
            NSLocalizedString(
                "Removed from Favorites",
                comment: "Favorite removed toast fallback text when more details are unavailable"
            )
        }

        toastVM.showToast(toast: .init(
            message: toastMessage,
            duration: .short,
            onClose: nil,
            actionLabel: NSLocalizedString("Undo", comment: "Button label to undo an action that was just performed"),
            onAction: {
                viewModel.updateFavorites(
                    updatedFavorites: [rsd: true],
                    context: .favorites,
                    defaultDirection: rsd.direction
                )
            },
        ))
    }

    var body: some View {
        ZStack {
            Color.sheetBackground.ignoresSafeArea(.all)
            VStack(alignment: .leading, spacing: 16) {
                SheetHeader(
                    title: NSLocalizedString("Edit Favorites", comment: "Title for flow to edit favorites"),
                    buttonColor: Color.key,
                    buttonTextColor: Color.fill3,
                    onClose: {
                        onClose()
                    },
                    closeText: NSLocalizedString("Done", comment: "Button text for closing flow")
                )
                EditFavoritesList(
                    routeCardData: favoritesVMState.staticRouteCardData,
                    global: globalResponse,
                    deleteFavorite: { rsd in deleteAndToast(rsd) }
                )
            }
            .onAppear {
                viewModel.setContext(context: FavoritesViewModel.ContextEdit())
                loadGlobal()
            }
            .onDisappear {
                toastVM.hideToast()
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
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
            HaloScrollView {
                LazyVStack(alignment: .center, spacing: 14) {
                    ForEach(routeCardData) { cardData in
                        RouteCardContainer(
                            cardData: cardData,
                            onPin: { _ in },
                            pinned: false,
                            showStopHeader: true
                        ) { stopData in
                            FavoriteDepartures(
                                stopData: stopData,
                                globalData: global
                            ) { leaf in
                                let favToDelete = RouteStopDirection(
                                    route: leaf.lineOrRoute.id,
                                    stop: leaf.stop.id,
                                    direction: leaf.directionId
                                )
                                deleteFavorite(favToDelete)
                            }
                        }
                    }
                }
                .padding(.vertical, 4)
                .padding(.horizontal, 16)
                .frame(maxHeight: .infinity, alignment: .top)
            }
        }

        else if routeCardData != nil {
            HaloScrollView {
                NoFavoritesView()
                    .frame(maxWidth: .infinity)
                    .padding(.top, 16)
            }
        }

        else {
            ScrollView([]) {
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

struct FavoriteDepartures: View {
    let stopData: RouteCardData.RouteStopData
    let globalData: GlobalResponse?
    let onClick: (RouteCardData.Leaf) -> Void

    var body: some View {
        VStack {
            ForEach(stopData.data.enumerated().sorted(by: { $0.offset < $1.offset }), id: \.element.id) { index, leaf in

                let formatted = leaf.format(now: EasternTimeInstant.now(), globalData: globalData)
                let direction: Direction = stopData.directions.first(where: { $0.id == leaf.directionId })!

                HStack(spacing: 0) {
                    switch onEnum(of: formatted) {
                    case let .single(single):
                        VStack(alignment: .center, spacing: 6) {
                            HStack(alignment: .center, spacing: 0) {
                                let pillDecoration: PredictionRowView.PillDecoration = if let route = single
                                    .route { .onRow(route: route) } else { .none }
                                DirectionLabel(
                                    direction: direction,
                                    showDestination: true,
                                    pillDecoration: pillDecoration
                                )
                                Spacer()
                                DeleteButton { onClick(leaf) }
                            }
                        }
                    case let .branched(branched):
                        HStack(alignment: .center, spacing: 0) {
                            VStack(alignment: .leading, spacing: 6) {
                                DirectionLabel(direction: direction, showDestination: false)
                                BranchRows(formatted: branched)
                            }.accessibilityElement(children: .combine)
                            Spacer()
                            DeleteButton { onClick(leaf) }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

                if index < stopData.data.endIndex {
                    HaloSeparator()
                }
            }
        }
    }
}

struct BranchRows: View {
    let formatted: LeafFormat.Branched

    var body: some View {
        VStack {
            ForEach(formatted.branchRows) { branch in
                HStack {
                    if case let route = branch.route {
                        RoutePill(
                            route: route,
                            line: nil,
                            type: .flex,
                        ).padding(.trailing, 8)
                    }
                    Text(
                        branch.headsign
                    ).font(Typography.bodySemibold)
                }
            }
        }
    }
}

struct DeleteButton: View {
    let action: () -> Void
    @ScaledMetric private var imageHeight = 16
    @ScaledMetric private var imageWidth = 14
    @ScaledMetric private var buttonSize = 44
    var body: some View {
        Button(action: { action()
        }) {
            ZStack {
                Circle()
                    .fill(Color.deleteBackground)
                    .frame(width: buttonSize, height: buttonSize)

                Image(.trashCan)
                    .resizable()
                    .scaledToFit()
                    .frame(width: imageWidth, height: imageHeight)
                    .foregroundColor(Color.delete)
            }
        }.accessibilityLabel(Text(
            "Delete",
            comment: "Content description for a button that removes a favorited route/stop/direction"
        ))
    }
}
