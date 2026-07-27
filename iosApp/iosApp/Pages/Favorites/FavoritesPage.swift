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
    @ObserveInjection var inject
    var alerts: AlertsStreamDataResponse?
    var errorBannerVM: IErrorBannerViewModel
    var favoritesVM: FavoritesViewModel
    var toastVM: IToastViewModel = ViewModelDI().toast
    @ObservedObject var navManager: NavigationManager
    @ObservedObject var viewportProvider: ViewportProvider

    var body: some View {
        ZStack {
            Color.sheetBackground.ignoresSafeArea(.all)
            FavoritesView(
                alerts: alerts,
                errorBannerVM: errorBannerVM,
                favoritesVM: favoritesVM,
                toastVM: toastVM,
                navManager: navManager,
                viewportProvider: viewportProvider,
            )
            .toolbarBackground(.visible, for: .tabBar)
        }
        .enableInjection()
    }
}
