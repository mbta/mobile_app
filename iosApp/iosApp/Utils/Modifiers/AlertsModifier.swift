//
//  AlertsModifier.swift
//  iosApp
//
//  Created by esimon on 6/16/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct AlertsModifier: ViewModifier {
    var alertsUsecase: AlertsUsecase = UsecaseDI().alertsUsecase
    @Binding var alerts: AlertsStreamDataResponse?

    func joinAlertsChannel() {
        alertsUsecase.connect { outcome in
            DispatchQueue.main.async {
                if case let .ok(result) = onEnum(of: outcome) {
                    alerts = result.data
                }
            }
        }
    }

    func leaveAlertsChannel() {
        alertsUsecase.disconnect()
    }

    func body(content: Content) -> some View {
        content
            .onAppear { joinAlertsChannel() }
            .withScenePhaseHandlers(
                onActive: { joinAlertsChannel() },
                onInactive: { leaveAlertsChannel() },
                onBackground: { leaveAlertsChannel() },
            )
            .onDisappear {
                leaveAlertsChannel()
            }
            .enableInjection()
    }
}

public extension View {
    func alerts(_ alerts: Binding<AlertsStreamDataResponse?>) -> some View {
        modifier(AlertsModifier(alerts: alerts))
    }
}
