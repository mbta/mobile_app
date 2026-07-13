//
//  NearbyTransitPage.swift
//  iosApp
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@_spi(Experimental) import MapboxMaps
import os
import Shared
import SwiftUI

struct NearbyTransitPage: View {
    @ObserveInjection var inject
    var alerts: AlertsStreamDataResponse?
    var errorBannerVM: IErrorBannerViewModel
    var nearbyVM: INearbyViewModel
    @ObservedObject var navManager: NavigationManager
    let noNearbyStops: () -> NoNearbyStopsView

    @State var location: CLLocationCoordinate2D?

    @EnvironmentObject var viewportProvider: ViewportProvider

    let inspection = Inspection<Self>()

    init(
        alerts: AlertsStreamDataResponse?,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: INearbyViewModel,
        navManager: NavigationManager,
        noNearbyStops: @escaping () -> NoNearbyStopsView,
    ) {
        self.alerts = alerts
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.navManager = navManager
        self.noNearbyStops = noNearbyStops
    }

    var body: some View {
        ZStack {
            Color.sheetBackground.ignoresSafeArea(.all)
            VStack(spacing: 0) {
                SheetHeader(
                    title: NSLocalizedString("Nearby Transit", comment: "Header for nearby transit sheet"),
                    navCallbacks: .init(onBack: nil, onClose: nil, backButtonPresentation: .floating)
                )
                .padding(.bottom, 16)
                ErrorBanner(errorBannerVM, padding: .init([.horizontal, .bottom], 16))
                DebugView { EmptyView() }
                NearbyTransitView(
                    alerts: alerts,
                    location: $location,
                    setIsReturningFromBackground: { errorBannerVM.setIsLoadingWhenPredictionsStale(isLoading: $0) },
                    noNearbyStops: noNearbyStops,
                    nearbyVM: nearbyVM,
                    navManager: navManager,
                    viewportProvider: viewportProvider,
                )
                .onReceive(
                    viewportProvider.cameraStatePublisher
                        .throttle(for: .seconds(2), scheduler: DispatchQueue.main, latest: true)

                ) { newCameraState in
                    guard navManager.isNearbyVisible() else { return }
                    if !newCameraState.center.isRoughlyEqualTo(location) {
                        location = newCameraState.center
                        print("KB: location updated")
                    }
                }
                .onReceive(inspection.notice) { inspection.visit(self, $0) }
            }
            .toolbarBackground(.visible, for: .tabBar)
            .onChange(of: viewportProvider.isManuallyCentering) { isManuallyCentering in
                if isManuallyCentering {
                    // The user is manually moving the map, clear the nearby state and
                    // reload it once the've stopped manipulating the map
                    location = nil
                }
            }
            .onChange(of: viewportProvider.isFollowingPuck) { isFollowingPuck in
                if isFollowingPuck {
                    // The user just recentered the map, clear the nearby state
                    location = nil
                }
            }
        }
        .enableInjection()
    }
}
