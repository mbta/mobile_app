//
//  AlertDetailsPage.swift
//  iosApp
//
//  Created by Simon, Emma on 8/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct AlertDetailsPage: View {
    var alertId: String
    var line: Line?
    var routes: [Route]?
    var stop: Stop?
    var nearbyVM: NearbyViewModel
    var globalRepository: IGlobalRepository = RepositoryDI().global

    @State private var alert: Shared.Alert?
    @State var globalResponse: GlobalResponse?
    @State private var now = Date.now

    @ScaledMetric private var modeIconHeight: CGFloat = 24
    @ScaledMetric private var elevatorIconHeight: CGFloat = 18
    private let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    let inspection = Inspection<Self>()

    private var affectedStops: [Stop] {
        globalResponse?.getAlertAffectedStops(alert: alert, routes: routes) ?? []
    }

    private var headerColor: Color {
        guard let lineHex = line?.color else {
            guard let routeHex = routes?.first?.color else {
                return Color.fill1
            }
            return Color(hex: routeHex)
        }
        return Color(hex: lineHex)
    }

    private var headerTextColor: Color {
        guard let lineHex = line?.textColor else {
            guard let routeHex = routes?.first?.textColor else {
                return Color.text
            }
            return Color(hex: routeHex)
        }
        return Color(hex: lineHex)
    }

    @ViewBuilder
    private var alertHeader: some View {
        HStack(alignment: .center, spacing: 6) {
            if let route = routes?.first {
                routeIcon(route)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .scaledToFit()
                    .frame(maxHeight: modeIconHeight, alignment: .topLeading)
            } else if alert?.effect == .elevatorClosure {
                AlertIcon(alertState: .elevator, accessibilityHidden: true)
                    .scaledToFit()
                    .frame(maxHeight: elevatorIconHeight, alignment: .topLeading)
            }
            Text("Alert Details", comment: "Header on the alert details page").font(Typography.headlineBold)
            Spacer()
            ActionButton(kind: .close) {
                nearbyVM.goBack()
            }
        }
        .padding(16)
        .foregroundStyle(headerTextColor)
        .background(headerColor)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            alertHeader
            if let alert {
                AlertDetails(
                    alert: alert,
                    line: line,
                    routes: routes,
                    stop: stop,
                    affectedStops: affectedStops,
                    stopId: nearbyVM.navigationStack.lastStopId,
                    now: now.toEasternInstant()
                )
            } else {
                ProgressView()
            }
        }
        .background(Color.fill2)
        .task {
            loadGlobal()
        }
        .onAppear { updateAlert() }
        .onChange(of: nearbyVM.alerts) { _ in updateAlert() }
        .onReceive(timer) { input in now = input }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            globalResponse = globalData
        }
    }

    private func loadGlobal() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorKey: "AlertDetailsPage.loadGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadGlobal
            )
        }
    }

    private func updateAlert() {
        guard let alerts = nearbyVM.alerts else { return }
        let nextAlert = alerts.getAlert(alertId: alertId)
        // If no alert is already set, and no alert was found with the provided ID,
        // something went wrong, and the alert didn't exist in the data to begin with,
        // navigate back to the previous page.
        if alert == nil, nextAlert == nil {
            nearbyVM.navigationStack.removeAll { nav in
                if case let .alertDetails(alertId, _, _, _) = nav {
                    alertId == self.alertId
                } else {
                    false
                }
            }
        }
        // If an alert is already set on the page, but doesn't exist in the data,
        // it probably expired while the page was open, so don't change anything and
        // keep displaying the expired alert as is.
        guard let nextAlert else { return }
        alert = nextAlert
    }
}
