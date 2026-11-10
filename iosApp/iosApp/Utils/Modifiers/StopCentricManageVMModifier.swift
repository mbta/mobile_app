//
//  StopCentricManageVMModifier.swift
//  iosApp
//
//  Created by esimon on 9/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopCentricManageVMModifier: ViewModifier {
    var viewModel: IStopCentricViewModel
    @Binding var vmState: StopCentricViewModel.State?
    var alerts: AlertsStreamDataResponse?
    var filters: StopCentricPageFilters
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
                onBackground: { viewModel.setActive(active: false, wasSentToBackground: true) }
            )
            .onDisappear {
                // Only set to inactive if the loaded filter matches the current value, this will be true
                // when the page is closed, but not when the loading view disappears or the filter is changed
                if let selectedFilters = vmState?.routeData?.filteredWith, filters == selectedFilters {
                    viewModel.setActive(active: false, wasSentToBackground: false)
                }
            }
            .enableInjection()
    }
}

public extension View {
    func manageVM(
        _ viewModel: IStopCentricViewModel,
        _ state: Binding<StopCentricViewModel.State?>,
        alerts: AlertsStreamDataResponse?,
        filters: StopCentricPageFilters,
        now: EasternTimeInstant
    ) -> some View {
        modifier(StopCentricManageVMModifier(
            viewModel: viewModel,
            vmState: state,
            alerts: alerts,
            filters: filters,
            now: now,
        ))
    }
}
