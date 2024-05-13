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

    init(departures: StopDetailsDepartures? = nil, navigationStack: [SheetNavigationStackEntry] = []) {
        self.departures = departures
        self.navigationStack = navigationStack
    }

    func setDepartures(_ newDepartures: StopDetailsDepartures?) {
        departures = newDepartures
    }

    func isNearbyVisible() -> Bool {
        navigationStack.isEmpty
    }
}
