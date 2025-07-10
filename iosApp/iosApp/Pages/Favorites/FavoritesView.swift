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
    var errorBannerVM: ErrorBannerViewModel
    var favoritesVM: IFavoritesViewModel
    @State var favoritesVMState: FavoritesViewModel.State = .init()
    @ObservedObject var nearbyVM: NearbyViewModel
    @Binding var location: CLLocationCoordinate2D?

    @State var globalData: GlobalResponse?
    var globalRepository = RepositoryDI().global
    let inspection = Inspection<Self>()
    @State var now = Date.now

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            SheetHeader(title: NSLocalizedString("Favorites", comment: "Header for favorites sheet"))
            ErrorBanner(errorBannerVM)
            RouteCardList(
                routeCardData: favoritesVMState.routeCardData,
                emptyView: { Text("No stops added", comment: "Indicates the absence of favorites") },
                global: globalData,
                now: now,
                isPinned: { _ in false },
                onPin: { _ in },
                pushNavEntry: { nearbyVM.pushNavEntry($0) },
                showStopHeader: true
            )
        }
        .onAppear {
            favoritesVM.setActive(active: true, wasSentToBackground: false)
            favoritesVM.setAlerts(alerts: nearbyVM.alerts)
            favoritesVM.setLocation(location: location?.positionKt)
            favoritesVM.setNow(now: now.toKotlinInstant())
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
            errorBannerVM.loadingWhenPredictionsStale = $0
        }
        .onChange(of: nearbyVM.alerts) { favoritesVM.setAlerts(alerts: $0) }
        .onChange(of: location?.positionKt) { favoritesVM.setLocation(location: $0) }
        .onChange(of: now) { favoritesVM.setNow(now: $0.toKotlinInstant()) }
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

    func getGlobal() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "FavoritesView.getGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadEverything
            )
        }
    }
}
