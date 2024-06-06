//
//  NearbyViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

class NearbyViewModel: ObservableObject {
    @Published var departures: StopDetailsDepartures?
    @Published var navigationStack: [SheetNavigationStackEntry] = []
    @Published var alerts: AlertsStreamDataResponse?

    private let alertsRepository: IAlertsRepository

    init(
        departures: StopDetailsDepartures? = nil,
        navigationStack: [SheetNavigationStackEntry] = [],
        alertsRepository: IAlertsRepository = RepositoryDI().alerts
    ) {
        self.departures = departures
        self.navigationStack = navigationStack
        self.alertsRepository = alertsRepository
        setUpSubscriptions()
    }

    func setDepartures(_ newDepartures: StopDetailsDepartures?) {
        departures = newDepartures
    }

    func isNearbyVisible() -> Bool {
        navigationStack.isEmpty
    }

    private func setUpSubscriptions() {
        alertsRepository.connect { outcome in
            DispatchQueue.main.async { [weak self] in
                if let data = outcome.data {
                    self?.alerts = data
                }
            }
        }
    }
}
