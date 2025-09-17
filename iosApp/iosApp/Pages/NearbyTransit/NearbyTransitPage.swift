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
    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    let noNearbyStops: () -> NoNearbyStopsView

    @State var location: CLLocationCoordinate2D?

    let inspection = Inspection<Self>()

    init(
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        viewportProvider: ViewportProvider,
        noNearbyStops: @escaping () -> NoNearbyStopsView
    ) {
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.viewportProvider = viewportProvider
        self.noNearbyStops = noNearbyStops
    }

    var body: some View {
        ZStack {
            Color.sheetBackground.ignoresSafeArea(.all)
            VStack(spacing: 0) {
                SheetHeader(title: NSLocalizedString("Nearby Transit", comment: "Header for nearby transit sheet"))
                    .padding(.bottom, 16)
                ErrorBanner(errorBannerVM, padding: .init([.horizontal, .bottom], 16))
                NearbyTransitView(
                    location: $location,
                    setIsReturningFromBackground: { errorBannerVM.setIsLoadingWhenPredictionsStale(isLoading: $0) },
                    nearbyVM: nearbyVM,
                    noNearbyStops: noNearbyStops
                )
                .onReceive(
                    viewportProvider.cameraStatePublisher
                        .debounce(for: .seconds(0.5), scheduler: DispatchQueue.main)

                ) { newCameraState in
                    guard nearbyVM.isNearbyVisible() else { return }
                    location = newCameraState.center
                }
                .onReceive(inspection.notice) { inspection.visit(self, $0) }
            }
            .toolbarBackground(.visible, for: .tabBar)
            .onChange(of: viewportProvider.isManuallyCentering) { isManuallyCentering in
                if isManuallyCentering {
                    // The user is manually moving the map, clear the nearby state and
                    // reload it once the've stopped manipulating the map
                    nearbyVM.clearNearbyData()
                    location = nil
                }
            }
            .onChange(of: viewportProvider.isFollowingPuck) { isFollowingPuck in
                if isFollowingPuck {
                    // The user just recentered the map, clear the nearby state
                    nearbyVM.clearNearbyData()
                    location = nil
                }
            }
        }
    }
}
