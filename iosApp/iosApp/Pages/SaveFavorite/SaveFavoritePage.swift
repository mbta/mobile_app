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
    var notificationPermissionManager: INotificationPermissionManager

    @State var globalResponse: GlobalResponse?
    @State var favorites: Favorites = .init(routeStopDirection: [:])
    @State var pendingSettings: MutableFavoriteSettings
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
        toastVM: IToastViewModel = ViewModelDI().toast,
        notificationPermissionManager: INotificationPermissionManager = NotificationPermissionManager(),
    ) {
        self.routeId = routeId
        self.stopId = stopId
        self.initialSelectedDirection = initialSelectedDirection
        self.context = context
        self.updateFavorites = updateFavorites
        self.navCallbacks = navCallbacks
        self.nearbyVM = nearbyVM
        self.toastVM = toastVM
        self.notificationPermissionManager = notificationPermissionManager
        pendingSettings = .init(.init())

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

    func resetPendingSettings() {
        pendingSettings = .init(
            favorites.routeStopDirection[selectedRouteStopDirection] ?? .init(notifications: .companion.disabled)
        )
    }

    func updateCloseAndToast(_ rsd: RouteStopDirection, _ setting: FavoriteSettings?) {
        updateFavorites([rsd: setting])

        navCallbacks.onBack?()

        // If the selected route, stop, and direction wasn't already favorited, show confirmation toast
        if !isFavorite {
            let toastText = if let labels = rsd.getLabels(globalResponse) {
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
            SaveFavoriteHeader(
                isFavorite: isFavorite,
                onCancel: { navCallbacks.onBack?() },
                onSave: { updateCloseAndToast(selectedRouteStopDirection, pendingSettings.toShared()) },
            )
            HaloScrollView(alwaysShowHalo: true) {
                if let lineOrRoute, let stop {
                    VStack(spacing: 22) {
                        FavoriteStopCard(
                            lineOrRoute: lineOrRoute,
                            stop: stop,
                            direction: stopDirections.first(where: { $0.id == selectedDirection }),
                            toggleDirection: (stopDirections.count > 1 && !isFavorite) ? {
                                selectedDirection = 1 - selectedDirection
                            } : nil,
                        )
                        NotificationSettingsWidget(
                            settings: pendingSettings.notifications,
                            notificationPermissionManager: notificationPermissionManager,
                        )
                        if isFavorite {
                            HaloSeparator(height: 2)
                            FavoriteDeleteButton {
                                deleteCloseAndToast(selectedRouteStopDirection)
                            }
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 24)
                } else {
                    Text(verbatim: "Placeholder loading")
                }
            }
        }
        .onAppear { resetPendingSettings() }
        .onChange(of: selectedDirection) { _ in resetPendingSettings() }
        .onChange(of: favorites) { _ in resetPendingSettings() }
        .frame(maxHeight: .infinity)
        .background(Color.fill2)
        .favorites($favorites)
        .global($globalResponse, errorKey: "SaveFavoritePage")
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}
