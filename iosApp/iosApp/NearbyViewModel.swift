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

    func setDepartures(_ newDepartures: StopDetailsDepartures?) {
        departures = newDepartures
    }
}
