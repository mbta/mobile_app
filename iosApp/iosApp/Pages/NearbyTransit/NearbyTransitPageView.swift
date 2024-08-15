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
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @State var location: CLLocationCoordinate2D?

    let inspection = Inspection<Self>()

    init(
        nearbyVM: NearbyViewModel,
        viewportProvider: ViewportProvider
    ) {
        self.nearbyVM = nearbyVM
        self.viewportProvider = viewportProvider
    }

    var body: some View {
        ZStack {
            Color.fill1.ignoresSafeArea(.all)
            VStack {
                SheetHeader(title: String(localized: "Nearby Transit", comment: "Header for nearby transit sheet"))
                if viewportProvider.isManuallyCentering {
                    LoadingCard { Text("select location") }
                } else {
                    NearbyTransitView(
                        getNearby: { global, location in
                            nearbyVM.getNearby(global: global, location: location)
                        },
                        state: $nearbyVM.nearbyState,
                        location: $location,
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
