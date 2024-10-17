//
//  NearbyTransitPageView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@_spi(Experimental) import MapboxMaps
import os
import shared
import SwiftUI

struct NearbyTransitPageView: View {
    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @State var location: CLLocationCoordinate2D?

    let inspection = Inspection<Self>()

    init(
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        viewportProvider: ViewportProvider
    ) {
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.viewportProvider = viewportProvider
    }

    var body: some View {
        ZStack {
            Color.fill1.ignoresSafeArea(.all)
            VStack(spacing: 16) {
                SheetHeader(title: String(localized: "Nearby Transit", comment: "Header for nearby transit sheet"))
                ErrorBanner(errorBannerVM).padding(.horizontal, 16)
                if viewportProvider.isManuallyCentering {
                    LoadingCard { Text("select location") }.padding(.horizontal, 16).padding(.bottom, 16)
                } else {
                    NearbyTransitView(
                        getNearby: { global, location in
                            nearbyVM.getNearby(global: global, location: location)
                        },
                        state: $nearbyVM.nearbyState,
                        location: $location,
                        isReturningFromBackground: $errorBannerVM.loadingWhenPredictionsStale,
                        nearbyVM: nearbyVM
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
            }
            .onChange(of: viewportProvider.isManuallyCentering) { isManuallyCentering in
                if isManuallyCentering {
                    // The user is manually moving the map, clear the nearby state and
                    // reload it once the've stopped manipulating the map
                    nearbyVM.nearbyState = .init()
                    location = nil
                }
            }
        }
    }
}
