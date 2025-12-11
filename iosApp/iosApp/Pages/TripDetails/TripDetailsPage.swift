//
//  TripDetailsPage.swift
//  iosApp
//
//  Created by esimon on 9/17/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TripDetailsPage: View {
    var filter: TripDetailsPageFilter
    var navCallbacks: NavigationCallbacks

    var errorBannerVM: IErrorBannerViewModel = ViewModelDI().errorBanner
    var mapVM: IMapViewModel = ViewModelDI().map
    var nearbyVM: NearbyViewModel
    var tripDetailsPageVM: ITripDetailsPageViewModel = ViewModelDI().tripDetailsPage
    var tripDetailsVM: ITripDetailsViewModel = ViewModelDI().tripDetails

    let analytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    @State var global: GlobalResponse?
    @State var now = Date.now.toEasternInstant()
    @State var tripDetailsPageState: TripDetailsPageViewModel.State?

    var alertSummaries: [String: AlertSummary?] {
        tripDetailsPageState?.alertSummaries as? [String: AlertSummary?] ?? [:]
    }

    var direction: Direction? { tripDetailsPageState?.direction }
    var route: Route? { global?.getRoute(routeId: tripDetailsPageState?.trip?.routeId ?? filter.routeId as? Route.Id) }
    var routeAccents: TripRouteAccents { if let route { .init(route: route) } else { .init() } }

    func onOpenAlertDetails(alert: Shared.Alert) {
        let routes: [Route]? = if let route { [route] } else { nil }
        nearbyVM.pushNavEntry(.alertDetails(
            alertId: alert.id,
            line: nil,
            routes: routes,
            stop: global?.getStop(stopId: filter.stopId)
        ))
        analytics.tappedAlertDetails(
            routeId: filter.routeId,
            stopId: filter.stopId,
            alertId: alert.id,
            elevator: false
        )
    }

    var body: some View {
        ZStack {
            routeAccents.color.ignoresSafeArea()
            VStack {
                TripDetailsHeader(route: route, direction: direction, navCallbacks: navCallbacks)
                ErrorBanner(errorBannerVM, padding: [.init(.bottom, 8), .init(.horizontal, 14)])
                HaloScrollView {
                    TripDetailsView(
                        tripFilter: filter,
                        alertSummaries: alertSummaries,
                        context: .tripDetails,
                        now: now,
                        onOpenAlertDetails: onOpenAlertDetails,
                        errorBannerVM: errorBannerVM,
                        nearbyVM: nearbyVM,
                        mapVM: mapVM,
                        tripDetailsVM: tripDetailsVM,
                    )
                }
            }
        }
        .manageVM(tripDetailsPageVM, $tripDetailsPageState, alerts: nearbyVM.alerts, filter: filter, now: now)
        .manageVM(
            tripDetailsVM,
            alerts: nearbyVM.alerts,
            context: .tripDetails,
            filters: filter,
        )
        .global($global, errorKey: "TripDetailsPage")
        .task {
            while !Task.isCancelled {
                now = Date.now.toEasternInstant()
                try? await Task.sleep(for: .seconds(5))
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}
