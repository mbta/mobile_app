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
    var currentLocation: CLLocationCoordinate2D?
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var scheduleFetcher: ScheduleFetcher
    @ObservedObject var predictionsFetcher: PredictionsFetcher
    @ObservedObject var viewportProvider: ViewportProvider
    @ObservedObject var alertsFetcher: AlertsFetcher

    @State var cancellables: [AnyCancellable]
    @StateObject var locationProvider: NearbyTransitLocationProvider

    let inspection = Inspection<Self>()

    init(
        currentLocation: CLLocationCoordinate2D?,
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        scheduleFetcher: ScheduleFetcher,
        predictionsFetcher: PredictionsFetcher,
        viewportProvider: ViewportProvider,
        alertsFetcher: AlertsFetcher
    ) {
        self.currentLocation = currentLocation
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.scheduleFetcher = scheduleFetcher
        self.predictionsFetcher = predictionsFetcher
        self.viewportProvider = viewportProvider
        self.alertsFetcher = alertsFetcher

        cancellables = .init()
        _locationProvider = StateObject(wrappedValue: .init(
            currentLocation: currentLocation,
            cameraLocation: viewportProvider.cameraState.center,
            isFollowing: viewportProvider.viewport.isFollowing
        ))
    }

    var body: some View {
        NearbyTransitView(
            location: locationProvider.location,
            globalFetcher: globalFetcher,
            nearbyFetcher: nearbyFetcher,
            scheduleFetcher: scheduleFetcher,
            predictionsFetcher: predictionsFetcher,
            alertsFetcher: alertsFetcher
        )
        .onAppear {
            cancellables.append(
                viewportProvider.$cameraState
                    .debounce(for: .seconds(0.5), scheduler: DispatchQueue.main)
                    .sink { newCameraState in
                        locationProvider.updateCameraLocation(newCameraState.center)
                    }
            )
        }
        .onChange(of: currentLocation) { newLocation in
            locationProvider.updateCurrentLocation(newLocation)
        }
        .onChange(of: viewportProvider.viewport) { newViewport in
            locationProvider.updateIsFollowing(
                newViewport.isFollowing,
                withCameraLocation: viewportProvider.cameraState.center
            )
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .navigationTitle("Nearby Transit")
        .navigationBarTitleDisplayMode(.inline)
    }
}
