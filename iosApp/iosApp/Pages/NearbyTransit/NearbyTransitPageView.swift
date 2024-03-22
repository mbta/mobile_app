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
    let currentLocation: CLLocationCoordinate2D?
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var scheduleFetcher: ScheduleFetcher
    @ObservedObject var predictionsFetcher: PredictionsFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @State var cancellables: [AnyCancellable]
    @State var locationProvider: NearbyTransitLocationProvider

    init(
        currentLocation: CLLocationCoordinate2D?,
        nearbyFetcher: NearbyFetcher,
        scheduleFetcher: ScheduleFetcher,
        predictionsFetcher: PredictionsFetcher,
        viewportProvider: ViewportProvider
    ) {
        self.currentLocation = currentLocation
        self.nearbyFetcher = nearbyFetcher
        self.scheduleFetcher = scheduleFetcher
        self.predictionsFetcher = predictionsFetcher
        self.viewportProvider = viewportProvider

        cancellables = .init()
        locationProvider = .init(
            currentLocation: currentLocation,
            cameraLocation: viewportProvider.cameraState.center,
            isFollowing: viewportProvider.viewport.isFollowing
        )
    }

    var body: some View {
        NearbyTransitView(
            locationProvider: locationProvider,
            nearbyFetcher: nearbyFetcher,
            scheduleFetcher: scheduleFetcher,
            predictionsFetcher: predictionsFetcher
        )
        .onAppear {
            cancellables.append(
                viewportProvider.$cameraState
                    .debounce(for: .seconds(0.5), scheduler: DispatchQueue.main)
                    .sink { newCameraState in
                        let shouldUpdateLocation = !viewportProvider.viewport.isFollowing &&
                            !locationProvider.location.isRoughlyEqualTo(newCameraState.center)
                        if shouldUpdateLocation {
                            locationProvider = .init(
                                currentLocation: currentLocation,
                                cameraLocation: newCameraState.center,
                                isFollowing: viewportProvider.viewport.isFollowing
                            )
                        }
                    }
            )
        }
        .onChange(of: currentLocation) { newLocation in
            let shouldUpdateLocation = viewportProvider.viewport.isFollowing
                && !locationProvider.location.isRoughlyEqualTo(newLocation)
            if shouldUpdateLocation {
                locationProvider = .init(
                    currentLocation: newLocation,
                    cameraLocation: viewportProvider.cameraState.center,
                    isFollowing: viewportProvider.viewport.isFollowing
                )
            }
        }
        .onChange(of: viewportProvider.viewport) { newViewport in
            locationProvider = .init(
                currentLocation: currentLocation,
                cameraLocation: viewportProvider.cameraState.center,
                isFollowing: newViewport.isFollowing
            )
        }
    }
}
