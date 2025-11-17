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
    var alerts: AlertsStreamDataResponse?
    var context: TripDetailsViewModel.Context
    var filters: TripDetailsPageFilter?

    @State var vmState: TripDetailsViewModel.State?

    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

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
            .onReceive(timer) { _ in
                viewModel.setActive(active: true, wasSentToBackground: false)
            }
            .onDisappear {
                // Only set to inactive if the loaded filter and context match the current state,
                // this will be true when the page is closed, but not when the loading view disappears,
                // the filter is changed, or the user navigates between stop/trip details
                if let selectedFilters = vmState?.tripData?.tripFilter,
                   filters == selectedFilters,
                   vmState?.context == context {
                    viewModel.setActive(active: false, wasSentToBackground: false)
                }
            }
    }
}

public extension View {
    func manageVM(
        _ viewModel: ITripDetailsViewModel,
        alerts: AlertsStreamDataResponse?,
        context: TripDetailsViewModel.Context,
        filters: TripDetailsPageFilter?,
    ) -> some View {
        modifier(TripDetailsManageVMModifier(
            viewModel: viewModel,
            alerts: alerts,
            context: context,
            filters: filters,
        ))
    }
}
