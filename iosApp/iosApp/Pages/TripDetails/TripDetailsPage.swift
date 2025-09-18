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
    var onClose: () -> Void

    var body: some View {
        VStack {
            SheetHeader(title: "Trip details", onClose: onClose)
            HaloScrollView {
                Text(verbatim: "trip id: \(filter.tripId)")
                Text(verbatim: "vehicle id: \(filter.vehicleId)")
            }
        }
    }
}
