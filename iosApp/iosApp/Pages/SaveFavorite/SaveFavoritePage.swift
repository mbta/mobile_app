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
    var selectedDirection: Int32
    var context: EditFavoritesContext

    let updateFavorites: ([RouteStopDirection: FavoriteSettings?]) -> Void
    var navCallbacks: NavigationCallbacks
    var nearbyVM: NearbyViewModel
    var toastVM: IToastViewModel = ViewModelDI().toast

    @State var globalResponse: GlobalResponse?
    @State var favorites: Favorites = .init(routeStopDirection: [:])
    @State var loadingFavorites: Bool = true
    @State var favoritesToSave: [Direction: FavoriteSettings?] = [:]

    let inspection = Inspection<Self>()

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

    var displayedDirection: Int32 {
        if stopDirections.count == 1, let direction = stopDirections.first {
            direction.id
        } else { selectedDirection }
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
        print("~~~ update SaveFavoritePage.updateCloseAndToast call")
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

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            saveFavoriteHeader
            HaloScrollView([], alwaysShowHalo: true) {
                if let lineOrRoute, let stop {
                    VStack {
                        FavoriteConfirmationDialogContents(
                            lineOrRoute: lineOrRoute,
                            stop: stop,
                            directions: stopDirections,
                            selectedDirection: displayedDirection,
                            context: context,
                            favoritesToSave: favoritesToSave,
                            updateLocalFavorite: { direction, isFavorite in
                                favoritesToSave[direction] = isFavorite
                            },
                        )
                    }.padding(16)
                } else {
                    Text(verbatim: "Placeholder loading")
                }
            }
        }
        .onAppear { setFavoritesToSave() }
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
                    Task(priority: .high) {
                        print("~~~ update tap Save button")
                        updateCloseAndToast(Dictionary(uniqueKeysWithValues: favoritesToSave.map { direction, setting in
                            (RouteStopDirection(route: routeId, stop: stopId, direction: direction.id), setting)
                        }))
                    }
                }
            }
            Text(isFavorite ? "Edit Favorite" : "Add Favorite").font(Typography.title1Bold).padding(.leading, 16)
        }
        .padding([.bottom, .trailing], 16)
        .foregroundStyle(Color.text)
        .background(Color.fill3)
    }
}
