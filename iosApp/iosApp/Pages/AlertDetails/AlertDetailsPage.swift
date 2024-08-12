//
//  AlertDetailsPage.swift
//  iosApp
//
//  Created by Simon, Emma on 8/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct AlertDetailsPage: View {
    var alertId: String
    var line: Line?
    var routes: [Route]?
    var nearbyVM: NearbyViewModel
    var globalRepository: IGlobalRepository = RepositoryDI().global

    @State private var alert: shared.Alert?
    @State var globalResponse: GlobalResponse?
    @State private var now = Date.now

    @ScaledMetric private var modeIconHeight: CGFloat = 24
    private let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    private var affectedStops: [Stop] {
        guard let globalResponse, let alert, let routes else { return [] }
        let routeEntities = alert.matchingEntities { entity in
            KotlinBoolean(bool: routes.contains { route in
                entity.route == nil || entity.route == route.id
            })
        }
        let parentStops: [Stop] = routeEntities
            .map { entity in entity.stop }
            .compactMap { stopId in
                guard let stopId else { return nil }
                let stop = globalResponse.stops[stopId]
                return stop?.resolveParent(stops: globalResponse.stops)
            }

        return NSOrderedSet(array: parentStops as [Any]).compactMap { $0 as? Stop }
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
            }
            Text("Alert Details").font(.headline)
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
                    affectedStops: affectedStops,
                    now: now
                )
            } else {
                ProgressView()
            }
        }
        .background(Color.fill2)
        .task {
            do {
                globalResponse = try await globalRepository.getGlobalData()
            } catch {
                debugPrint(error)
            }
        }
        .onAppear {
            updateAlert()
        }
        .onChange(of: nearbyVM.alerts) { _ in
            updateAlert()
        }
        .onReceive(timer) { input in
            now = input
        }
    }

    private func updateAlert() {
        guard let alerts = nearbyVM.alerts else { return }
        alert = alerts.alerts[alertId]
        if alert == nil {
            nearbyVM.navigationStack.removeAll { nav in
                if case let .alertDetails(alertId, _, _) = nav {
                    alertId == self.alertId
                } else {
                    false
                }
            }
        }
    }
}
