//
//  TripDetailsPageManageVMModifier.swift
//  iosApp
//
//  Created by esimon on 9/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TripDetailsPageManageVMModifier: ViewModifier {
    var viewModel: ITripDetailsPageViewModel
    @Binding var vmState: TripDetailsPageViewModel.State?
    var alerts: AlertsStreamDataResponse?
    var filter: TripDetailsPageFilter?
    var now: EasternTimeInstant

    func body(content: Content) -> some View {
        content
            .task {
                for await models in viewModel.models {
                    vmState = models
                }
            }
            .onAppear {
                viewModel.setAlerts(alerts: alerts)
                viewModel.setFilter(filter: filter)
                viewModel.setNow(now: now)
            }
            .onChange(of: alerts) { viewModel.setAlerts(alerts: $0) }
            .onChange(of: filter) { viewModel.setFilter(filter: $0) }
            .onChange(of: now) { viewModel.setNow(now: $0) }
    }
}

public extension View {
    func manageVM(
        _ viewModel: ITripDetailsPageViewModel,
        _ state: Binding<TripDetailsPageViewModel.State?>,
        alerts: AlertsStreamDataResponse?,
        filter: TripDetailsPageFilter?,
        now: EasternTimeInstant
    ) -> some View {
        modifier(TripDetailsPageManageVMModifier(
            viewModel: viewModel,
            vmState: state,
            alerts: alerts,
            filter: filter,
            now: now,
        ))
    }
}
