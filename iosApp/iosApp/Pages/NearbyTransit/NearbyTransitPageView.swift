//
//  NearbyTransitPageView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
import os
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct NearbyTransitPageView: View {
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @State var location: CLLocationCoordinate2D?

    let inspection = Inspection<Self>()

    init(
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        nearbyVM: NearbyViewModel,
        viewportProvider: ViewportProvider
    ) {
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.nearbyVM = nearbyVM
        self.viewportProvider = viewportProvider
    }

    var body: some View {
        ZStack {
            Color.fill1.ignoresSafeArea(.all)
            NearbyTransitView(
                location: location,
                alerts: nearbyVM.alerts,
                globalFetcher: globalFetcher,
                nearbyFetcher: nearbyFetcher
            )
            .onReceive(
                viewportProvider.cameraStatePublisher
                    .debounce(for: .seconds(0.5), scheduler: DispatchQueue.main)
            ) { newCameraState in
                guard nearbyVM.isNearbyVisible() else { return }
                location = newCameraState.center
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .navigationTitle("Nearby Transit")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
