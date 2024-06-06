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
    var schedulesRepository: ISchedulesRepository
    @ObservedObject var viewportProvider: ViewportProvider
    @ObservedObject var alertsFetcher: AlertsFetcher

    @State var location: CLLocationCoordinate2D?

    let inspection = Inspection<Self>()

    init(
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        nearbyVM: NearbyViewModel,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        viewportProvider: ViewportProvider,
        alertsFetcher: AlertsFetcher
    ) {
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.nearbyVM = nearbyVM
        self.schedulesRepository = schedulesRepository
        self.viewportProvider = viewportProvider
        self.alertsFetcher = alertsFetcher
    }

    var body: some View {
        ZStack {
            Color.fill1.ignoresSafeArea(.all)
            NearbyTransitView(
                location: location,
                globalFetcher: globalFetcher,
                nearbyFetcher: nearbyFetcher,
                nearbyVM: nearbyVM,
                schedulesRepository: schedulesRepository,
                alertsFetcher: alertsFetcher
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
