//
//  LoadingResultsView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct LoadingResultsView: View {
    @ObserveInjection var inject
    var body: some View {
        StopResultsView(stops: LoadingPlaceholders.shared.stopResults(), handleStopTap: { _ in })
            .loadingPlaceholder()
            .enableInjection()
    }
}
