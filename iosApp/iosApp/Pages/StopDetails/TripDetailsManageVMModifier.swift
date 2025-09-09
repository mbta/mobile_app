//
//  TripDetailsManageVMModifier.swift
//  iosApp
//
//  Created by esimon on 9/8/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TripDetailsManageVMModifier: ViewModifier {
    var viewModel: ITripDetailsViewModel
    @Binding var vmState: TripDetailsViewModel.State?
    var alerts: AlertsStreamDataResponse?
    var context: TripDetailsViewModel.Context
    var filters: TripDetailsPageFilter?

    func body(content: Content) -> some View {
        content
            .task {
                for await models in viewModel.models {
                    vmState = models
                }
            }
            .onAppear {
                viewModel.setAlerts(alerts: alerts)
                viewModel.setContext(context: context)
                viewModel.setFilters(filters: filters)
                viewModel.setActive(active: true, wasSentToBackground: false)
            }
            .onChange(of: alerts) { newAlerts in
                viewModel.setAlerts(alerts: newAlerts)
            }
            .onChange(of: context) { newContext in
                viewModel.setContext(context: newContext)
            }
            .onChange(of: filters) { newFilters in
                viewModel.setFilters(filters: newFilters)
            }
            .withScenePhaseHandlers(
                onActive: { viewModel.setActive(active: true, wasSentToBackground: false) },
                onInactive: { viewModel.setActive(active: false, wasSentToBackground: false) },
                onBackground: { viewModel.setActive(active: false, wasSentToBackground: true) },
            )
    }
}

public extension View {
    func manageVM(
        _ viewModel: ITripDetailsViewModel,
        _ state: Binding<TripDetailsViewModel.State?>,
        alerts: AlertsStreamDataResponse?,
        context: TripDetailsViewModel.Context,
        filters: TripDetailsPageFilter?,
    ) -> some View {
        modifier(TripDetailsManageVMModifier(
            viewModel: viewModel,
            vmState: state,
            alerts: alerts,
            context: context,
            filters: filters,
        ))
    }
}
