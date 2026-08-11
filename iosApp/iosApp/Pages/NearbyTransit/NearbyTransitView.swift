//
//  NearbyTransitView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-19.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@_spi(Experimental) import MapboxMaps
import os
import Shared
import SwiftUI

struct NearbyTransitView: View {
    @ObserveInjection var inject

    var alerts: AlertsStreamDataResponse?
    var initialLocation: CLLocationCoordinate2D?
    let setIsReturningFromBackground: (Bool) -> Void
    let noNearbyStops: () -> NoNearbyStopsView
    var nearbyVM: INearbyViewModel
    @ObservedObject var navManager: NavigationManager
    @ObservedObject var viewportProvider: ViewportProvider

    var analytics: Analytics = AnalyticsProvider.shared

    @State var favorites: Favorites = LoadedFavorites.last
    @State var globalData: GlobalResponse?
    @State var now = EasternTimeInstant.now()
    @State var nearbyTransitState: Shared.NearbyViewModel.State?

    @State var location: CLLocationCoordinate2D?
    @State var locationUnthrottled: CLLocationCoordinate2D?

    private let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    let inspection = Inspection<Self>()
    let scrollSubject = PassthroughSubject<LineOrRoute.Id, Never>()

    var body: some View {
        VStack(spacing: 0) {
            if let routeCardData = nearbyTransitState?.routeCardData,
               let global = globalData {
                nearbyList(routeCardData, global)
                    .onAppear { didLoadData?(self) }
            } else {
                loadingBody()
            }
        }
        .favorites($favorites)
        .global($globalData, errorKey: .companion.fromSheetTypes(sheetTypes: [.nearbyTransit], id: "NearbyTransitView"))
        .onAppear {
            nearbyVM.setActive(active: true, wasSentToBackground: false)
            nearbyVM.setAlerts(alerts: alerts)
            nearbyVM.setLocation(location: location?.positionKt ?? initialLocation?.positionKt)
            nearbyVM.setNow(now: now)
            didAppear?(self)
        }
        .task { for await state in nearbyVM.models {
            nearbyTransitState = state
        } }
        .onChange(of: alerts) { alerts in nearbyVM.setAlerts(alerts: alerts) }
        .onChange(of: now) { now in nearbyVM.setNow(now: now) }
        .onChange(of: nearbyTransitState?.loadedLocation) { loadedLocation in
            if let loadedLocation, let currentPosition = locationUnthrottled?.positionKt,
               loadedLocation.isRoughlyEqualTo(other: currentPosition) {
                viewportProvider.lastLoadedLocation = loadedLocation.coordinate
                viewportProvider.isTargeting = false
            }
        }
        .onChange(of: nearbyTransitState?.awaitingPredictionsAfterBackground) { awaitingPredictions in
            if let awaitingPredictions { setIsReturningFromBackground(awaitingPredictions) }
        }
        .onChange(of: viewportProvider.isManuallyCentering) { isCentering in
            if let location, !isCentering, navManager.isNearbyVisible() {
                nearbyVM.setLocation(location: location.positionKt)
            }
        }
        .onReceive(viewportProvider.cameraStatePublisherThrottled) { newCameraState in
            location = newCameraState.center
            guard navManager.isNearbyVisible(), !viewportProvider.isManuallyCentering, let location else { return }
            nearbyVM.setLocation(location: location.positionKt)
        }
        .onReceive(viewportProvider.cameraStatePublisher) { newCameraState in
            guard navManager.isNearbyVisible() else { return }
            locationUnthrottled = newCameraState.center
        }
        .onReceive(timer) { input in now = input.toEasternInstant() }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onDisappear { nearbyVM.setActive(active: false, wasSentToBackground: false) }
        .withScenePhaseHandlers(
            onActive: { nearbyVM.setActive(active: true, wasSentToBackground: false) },
            onInactive: { nearbyVM.setActive(active: false, wasSentToBackground: false) },
            onBackground: { nearbyVM.setActive(active: false, wasSentToBackground: true) }
        )
        .enableInjection()
    }

    @ViewBuilder private func nearbyList(_ routeCardData: [RouteCardData], _ global: GlobalResponse) -> some View {
        if routeCardData.isEmpty {
            ScrollView {
                noNearbyStops().padding(.horizontal, 16)
                Spacer()
            }
        } else {
            ScrollViewReader { proxy in
                HaloScrollView {
                    LazyVStack(spacing: 18) {
                        ForEach(routeCardData) { cardData in
                            RouteCard(
                                cardData: cardData,
                                global: global,
                                now: now,
                                isFavorite: { favorites.isFavorite($0) },
                                pushNavEntry: { entry in navManager.pushNavEntry(entry) },
                                showStopHeader: true
                            )
                        }
                    }
                    .padding(.vertical, 4)
                    .padding(.horizontal, 16)
                }
                .onReceive(scrollSubject) { id in
                    withAnimation {
                        proxy.scrollTo(id, anchor: .top)
                    }
                }
            }
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        ScrollView([]) {
            LazyVStack(spacing: 18) {
                ForEach(1 ... 5, id: \.self) { _ in
                    RouteCard(
                        cardData: LoadingPlaceholders.shared.nearbyRoute(),
                        global: globalData,
                        now: now,
                        isFavorite: { _ in false },
                        pushNavEntry: { _ in },
                        showStopHeader: true
                    )
                    .loadingPlaceholder(withShimmer: false)
                }
            }
            .padding(.vertical, 4)
            .padding(.horizontal, 16)
        }
    }

    var didAppear: ((Self) -> Void)?
    var didLoadData: ((Self) -> Void)?

    private func scrollToTop() {
        guard let id = nearbyTransitState?.routeCardData?.first?.lineOrRoute.id else { return }
        scrollSubject.send(id)
    }
}
