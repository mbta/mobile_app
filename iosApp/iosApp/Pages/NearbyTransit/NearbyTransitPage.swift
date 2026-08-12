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
                    setIsReturningFromBackground: { errorBannerVM.setIsLoadingWhenPredictionsStale(isLoading: $0) },
                    noNearbyStops: noNearbyStops,
                    nearbyVM: nearbyVM,
                    navManager: navManager,
                    viewportProvider: viewportProvider,
                )
                .onReceive(inspection.notice) { inspection.visit(self, $0) }
            }
            .toolbarBackground(.visible, for: .tabBar)
        }
        .enableInjection()
    }
}
