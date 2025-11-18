//
//  SaveFavoritePage.swift
//  iosApp
//
//  Created by esimon on 11/4/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct SaveFavoritePage: View {
    var routeId: LineOrRoute.Id
    var stopId: String
    var initialSelectedDirection: Int32
    var context: EditFavoritesContext

    let updateFavorites: ([RouteStopDirection: FavoriteSettings?]) -> Void
    var navCallbacks: NavigationCallbacks
    var nearbyVM: NearbyViewModel
    var toastVM: IToastViewModel

    @State var globalResponse: GlobalResponse?
    @State var favorites: Favorites = .init(routeStopDirection: [:])
    @State var loadingFavorites: Bool = true
    @State var favoritesToSave: [Direction: FavoriteSettings?] = [:]
    @State var selectedDirection: Int32

    let inspection = Inspection<Self>()

    init(
        routeId: LineOrRoute.Id,
        stopId: String,
        initialSelectedDirection: Int32,
        context: EditFavoritesContext,
        updateFavorites: @escaping ([RouteStopDirection: FavoriteSettings?]) -> Void,
        navCallbacks: NavigationCallbacks,
        nearbyVM: NearbyViewModel,
        toastVM: IToastViewModel = ViewModelDI().toast
    ) {
        self.routeId = routeId
        self.stopId = stopId
        self.initialSelectedDirection = initialSelectedDirection
        self.context = context
        self.updateFavorites = updateFavorites
        self.navCallbacks = navCallbacks
        self.nearbyVM = nearbyVM
        self.toastVM = toastVM

        selectedDirection = initialSelectedDirection
    }

    var lineOrRoute: LineOrRoute? { globalResponse?.getLineOrRoute(lineOrRouteId: routeId) }
    var stop: Stop? { globalResponse?.getStop(stopId: stopId) }

    var selectedRouteStopDirection: RouteStopDirection {
        RouteStopDirection(route: routeId, stop: stopId, direction: selectedDirection)
    }

    var isFavorite: Bool { favorites.isFavorite(selectedRouteStopDirection) }
    var allPatternsForStop: [RoutePattern] {
        if let globalResponse, let lineOrRoute, let stop {
            globalResponse.getPatternsFor(stopId: stop.id, lineOrRoute: lineOrRoute)
        } else { [] }
    }

    var stopDirections: [Direction] {
        if let globalResponse, let lineOrRoute, let stop {
            lineOrRoute.directions(
                globalData: globalResponse,
                stop: stop,
                patterns: allPatternsForStop.filter { $0.isTypical() }
            ).filter {
                RouteDetailsStopList.RouteParameters(lineOrRoute: lineOrRoute, globalData: globalResponse)
                    .availableDirections.contains(KotlinInt(value: $0.id)) &&
                    !stop.isLastStopForAllPatterns(
                        directionId: $0.id,
                        patterns: allPatternsForStop,
                        global: globalResponse
                    )
            }
        } else { [] }
    }

    func setFavoritesToSave() {
        favoritesToSave = Dictionary(uniqueKeysWithValues: stopDirections.map { direction in
            let existingFavorite = favorites.routeStopDirection[RouteStopDirection(
                route: routeId,
                stop: stopId,
                direction: direction.id
            )]
            let directionFavorite: FavoriteSettings? = if let existingFavorite {
                existingFavorite
            } else {
                direction.id == selectedRouteStopDirection.direction ? .init(notifications: .companion.disabled) : nil
            }
            return (direction, directionFavorite)
        })
    }

    func updateCloseAndToast(_ update: [RouteStopDirection: FavoriteSettings?]) {
        updateFavorites(update)

        let favorited = update.filter { $0.value != nil }
        let firstFavorite = favorited.first
        let labels = firstFavorite?.key.getLabels(globalResponse)
        var toastText: String? = nil

        // If there's only a single favorite, show direction, route, and stop in the toast
        if favorited.count == 1 {
            toastText = if let labels {
                String(format: NSLocalizedString(
                    "**%1$@ %2$@** at **%3$@** added to Favorites",
                    comment: """
                    Favorite added toast text, the first value is the direction (southbound, inbound, etc),
                    the second is the route name (Red Line, 1 bus), and the third is a stop name (Ruggles, Alewife).
                    The asterisks surround bolded text. ex. "[Southbound] [Red Line] at [Alewife] added to Favorites"
                    """
                ), labels.direction, labels.route, labels.stop)
            } else {
                NSLocalizedString(
                    "Added to Favorites",
                    comment: "Favorites added toast fallback text when more details are unavailable"
                )
            }
        }
        // If there are two favorites and they both have the same route and stop, omit direction
        else if favorited.count == 2,
                favorited.keys.allSatisfy({
                    $0.route == firstFavorite?.key.route
                        && $0.stop == firstFavorite?.key.stop
                }) {
            toastText = if let labels {
                String(format: NSLocalizedString(
                    "**%1$@** at **%2$@** added to Favorites",
                    comment: """
                    Favorite added toast text, the first value is the route name (Red Line, 1 bus),
                    and the second is a stop name (Ruggles, Alewife).
                    The asterisks surround bolded text. ex. "[Green Line] at [Arlington] added to Favorites"
                    """
                ), labels.route, labels.stop)
            } else {
                NSLocalizedString(
                    "Added to Favorites",
                    comment: "Favorites added toast fallback text when more details are unavailable"
                )
            }
        }

        navCallbacks.onBack?()

        if let toastText {
            toastVM.showToast(toast: .init(
                message: toastText,
                duration: .short, isTip: false, action: nil
            ))
        }
    }

    func deleteCloseAndToast(_ rsd: RouteStopDirection) {
        let settings = favorites.routeStopDirection[rsd]
        updateFavorites([rsd: nil])

        let labels = rsd.getLabels(globalResponse)
        let toastMessage = if let labels {
            String(format: NSLocalizedString(
                "**%1$@ %2$@** at **%3$@** removed from Favorites",
                comment: """
                Favorite removed toast text, the first value is the direction (southbound, inbound, etc),
                the second is the route name (Red Line, 1 bus), and the third is a stop name (Ruggles, Alewife).
                The asterisks surround bolded text. ex. \"[Outbound] [71 bus] at [Harvard] removed from Favorites\"
                """
            ), labels.direction, labels.route, labels.stop)
        } else {
            NSLocalizedString(
                "Removed from Favorites",
                comment: "Favorite removed toast fallback text when more details are unavailable"
            )
        }

        navCallbacks.onBack?()

        toastVM.showToast(toast: .init(
            message: toastMessage,
            duration: .short,
            isTip: false,
            action: ToastViewModel.ToastActionCustom(
                actionLabel: NSLocalizedString(
                    "Undo",
                    comment: "Button label to undo an action that was just performed"
                ),
                onAction: {
                    updateFavorites([rsd: settings])
                }
            ),
        ))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            saveFavoriteHeader
            HaloScrollView([], alwaysShowHalo: true) {
                if let lineOrRoute, let stop {
                    VStack(spacing: 24) {
                        stopDirectionCard(lineOrRoute, stop)
                        if isFavorite {
                            HaloSeparator(height: 2)
                            deleteButton
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 22)
                } else {
                    Text(verbatim: "Placeholder loading")
                }
            }
        }
        .onAppear {
            setFavoritesToSave()
            selectedDirection = initialSelectedDirection
        }
        .onChange(of: stopDirections) { _ in setFavoritesToSave() }
        .frame(maxHeight: .infinity)
        .background(Color.fill2)
        .favorites($favorites)
        .global($globalResponse, errorKey: "SaveFavoritePage")
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder
    private var saveFavoriteHeader: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .center, spacing: 6) {
                NavTextButton(string: "Cancel", backgroundColor: Color.clear, textColor: Color.key) {
                    navCallbacks.onBack?()
                }.padding(.leading, 4)
                Spacer()
                NavTextButton(string: "Save", backgroundColor: Color.key, textColor: Color.fill3) {
                    updateCloseAndToast(Dictionary(uniqueKeysWithValues: favoritesToSave.map { direction, setting in
                        (RouteStopDirection(route: routeId, stop: stopId, direction: direction.id), setting)
                    }))
                }
            }
            Text(isFavorite ? "Edit Favorite" : "Add Favorite").font(Typography.title1Bold).padding(.leading, 16)
        }
        .padding([.bottom, .trailing], 16)
        .foregroundStyle(Color.text)
        .background(Color.fill3)
    }

    @ViewBuilder
    private func stopDirectionCard(_ lineOrRoute: LineOrRoute, _ stop: Stop) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: 12) {
                (stop.locationType == .stop ? Image(.mapStopCloseBUS) : Image(.mbtaLogo)).resizable().scaledToFit()
                    .frame(
                        width: 24,
                        height: 24
                    ).accessibilityHidden(true)
                Text(stop.name).font(Typography.bodySemibold)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(Color.halo)
            HStack(alignment: .center, spacing: 8) {
                RoutePill(lineOrRoute: lineOrRoute, type: .fixed)
                if let direction = stopDirections.first { $0.id == selectedDirection } {
                    DirectionLabel(direction: direction).frame(maxWidth: .infinity, alignment: .leading)
                }
                if stopDirections.count > 1, !isFavorite {
                    ActionButton(kind: .exchange) {
                        selectedDirection = 1 - selectedDirection
                    }
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 10)
        }
        .background(Color.fill3)
        .withRoundedBorder(width: 2)
    }

    @ViewBuilder
    private var deleteButton: some View {
        Button(
            action: { deleteCloseAndToast(selectedRouteStopDirection) },
            label: {
                HStack(alignment: .center, spacing: 16) {
                    Text("Remove from Favorites", comment: "Button to delete an individual favorite")
                        .font(Typography.bodySemibold)
                    Image(.trashCan).resizable().scaledToFit().frame(width: 24, height: 24)
                }.frame(maxWidth: .infinity, alignment: .center)
            }
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color.delete)
        .foregroundStyle(Color.deleteBackground)
        .withRoundedBorder(width: 0)
    }
}
