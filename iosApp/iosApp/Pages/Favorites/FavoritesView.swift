//
//  FavoritesView.swift
//  iosApp
//
//  Created by Melody Horn on 6/12/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
import Shared
import SwiftUI

struct FavoritesView: View {
    var errorBannerVM: IErrorBannerViewModel
    var favoritesVM: IFavoritesViewModel
    @State var favoritesVMState: FavoritesViewModel.State = .init()
    @ObservedObject var nearbyVM: NearbyViewModel
    var toastVM: IToastViewModel
    @Binding var location: CLLocationCoordinate2D?

    @State var globalData: GlobalResponse?
    var globalRepository = RepositoryDI().global
    let inspection = Inspection<Self>()
    @State var now = Date.now

    // Needed in addition to the VM for compatibility with fetchApi
    let errorBannerRepository = RepositoryDI().errorBanner

    @ScaledMetric private var editButtonHeight: CGFloat = 32

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(
                title: NSLocalizedString("Favorites", comment: "Header for favorites sheet"),
                rightActionContents: {
                    if let routeCardData = favoritesVMState.routeCardData, !routeCardData.isEmpty {
                        ActionButton(kind: .plus, circleColor: Color.translucentContrast, action: { onAddStops() })
                        NavTextButton(
                            string: NSLocalizedString("Edit", comment: "Button text to enter edit favorites flow"),
                            backgroundColor: Color.translucentContrast,
                            textColor: Color.fill2,
                            height: editButtonHeight,
                            width: 64
                        ) {
                            nearbyVM.pushNavEntry(.editFavorites)
                        }
                    }
                }
            )

            ErrorBanner(errorBannerVM)
            RouteCardList(
                routeCardData: favoritesVMState.routeCardData,
                emptyView: {
                    NoFavoritesView(
                        onAddStops: {
                            onAddStops()
                        }
                    )
                    .frame(maxWidth: .infinity)
                },
                global: globalData,
                now: now,
                isFavorite: { rsd in favoritesVMState.favorites?.contains(where: { rsd == $0 }) ?? false },
                pushNavEntry: { nearbyVM.pushNavEntry($0) },
                showStopHeader: true
            )
        }
        .onAppear {
            favoritesVM.setActive(active: true, wasSentToBackground: false)
            favoritesVM.setAlerts(alerts: nearbyVM.alerts)
            favoritesVM.setContext(context: FavoritesViewModel.ContextFavorites())
            favoritesVM.setLocation(location: location?.positionKt)
            favoritesVM.setNow(now: now.toEasternInstant())
            favoritesVM.reloadFavorites()
            loadEverything()
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .task {
            while !Task.isCancelled {
                now = Date.now
                try? await Task.sleep(for: .seconds(5))
            }
        }
        .task {
            for await model in favoritesVM.models {
                favoritesVMState = model
            }
        }
        .onChange(of: favoritesVMState.awaitingPredictionsAfterBackground) {
            errorBannerVM.setIsLoadingWhenPredictionsStale(isLoading: $0)
        }
        .onChange(of: favoritesVMState.loadedLocation) {
            nearbyVM.lastLoadedLocation = $0?.coordinate
            nearbyVM.isTargeting = false
        }
        .onAppear {
            if favoritesVMState.shouldShowFirstTimeToast {
                showFirstTimeToast()
            }
        }
        .onDisappear {
            toastVM.hideToast()
        }
        .onChange(of: favoritesVMState.shouldShowFirstTimeToast) { shouldShow in
            if shouldShow {
                showFirstTimeToast()
            }
        }
        .onChange(of: nearbyVM.alerts) { favoritesVM.setAlerts(alerts: $0) }
        .onChange(of: location?.positionKt) { favoritesVM.setLocation(location: $0) }
        .onChange(of: now) { favoritesVM.setNow(now: $0.toEasternInstant()) }
        .withScenePhaseHandlers(
            onActive: { favoritesVM.setActive(active: true, wasSentToBackground: false) },
            onInactive: { favoritesVM.setActive(active: false, wasSentToBackground: false) },
            onBackground: { favoritesVM.setActive(active: false, wasSentToBackground: true) }
        )
    }

    private func loadEverything() {
        getGlobal()
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            self.globalData = globalData
        }
    }

    func showFirstTimeToast() {
        toastVM.showToast(toast:
            .init(message:
                NSLocalizedString("Favorite stops replaces the prior starred routes feature.",
                                  comment: "Explainer the first time a user sees the new favorites feature"),
                duration: .indefinite,
                isTip: false,
                action: ToastViewModel
                    .ToastActionClose(onClose: { favoritesVM.setIsFirstExposureToNewFavorites(isFirst: false)
                        toastVM.hideToast()
                    })))
    }

    func getGlobal() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "FavoritesView.getGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadEverything
            )
        }
    }

    private func onAddStops() {
        favoritesVM.setIsFirstExposureToNewFavorites(isFirst: false)
        toastVM.hideToast()
        nearbyVM.pushNavEntry(
            SheetNavigationStackEntry.routePicker(
                SheetRoutes.RoutePicker(
                    path: RoutePickerPath.Root(),
                    context: RouteDetailsContext.Favorites()
                )
            )
        )
    }
}
