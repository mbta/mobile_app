//
//  StopDetailsManageVMModifier.swift
//  iosApp
//
//  Created by esimon on 9/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopDetailsManageVMModifier: ViewModifier {
    var viewModel: IStopDetailsViewModel
    @Binding var vmState: StopDetailsViewModel.State?
    var alerts: AlertsStreamDataResponse?
    var filters: StopDetailsPageFilters
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
                viewModel.setFilters(filters: filters)
                viewModel.setNow(now: now)
                viewModel.setActive(active: true, wasSentToBackground: false)
            }
            .onChange(of: alerts) { newAlerts in
                viewModel.setAlerts(alerts: newAlerts)
            }
            .onChange(of: filters) { newFilters in
                viewModel.setFilters(filters: newFilters)
            }
            .onChange(of: now) { nextNow in
                viewModel.setNow(now: nextNow)
            }
            .withScenePhaseHandlers(
                onActive: { viewModel.setActive(active: true, wasSentToBackground: false) },
                onInactive: { viewModel.setActive(active: false, wasSentToBackground: false) },
                onBackground: { viewModel.setActive(active: false, wasSentToBackground: true) },
            )
            .onDisappear {
                // Only set to inactive if the loaded filter matches the current value, this will be true
                // when the page is closed, but not when the loading view disappears or the filter is changed
                if let selectedFilters = vmState?.routeData?.filters, filters == selectedFilters {
                    viewModel.setActive(active: false, wasSentToBackground: false)
                }
            }
    }
}

public extension View {
    func manageVM(
        _ viewModel: IStopDetailsViewModel,
        _ state: Binding<StopDetailsViewModel.State?>,
        alerts: AlertsStreamDataResponse?,
        filters: StopDetailsPageFilters,
        now: EasternTimeInstant
    ) -> some View {
        modifier(StopDetailsManageVMModifier(
            viewModel: viewModel,
            vmState: state,
            alerts: alerts,
            filters: filters,
            now: now,
        ))
    }
}
