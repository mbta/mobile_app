//
//  FavoritesPage.swift
//  iosApp
//
//  Created by esimon on 5/15/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
import Shared
import SwiftUI

struct FavoritesPage: View {
    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    var favoritesVM: FavoritesViewModel
    var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @State var location: CLLocationCoordinate2D?

    var body: some View {
        ZStack {
            Color.sheetBackground.ignoresSafeArea(.all)
            FavoritesView(
                errorBannerVM: errorBannerVM,
                favoritesVM: favoritesVM,
                nearbyVM: nearbyVM,
                location: $location
            )
            .onReceive(
                viewportProvider.cameraStatePublisher
                    .debounce(for: .seconds(0.5), scheduler: DispatchQueue.main)

            ) { newCameraState in
                guard nearbyVM.isFavoritesVisible() else { return }
                location = newCameraState.center
            }
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
