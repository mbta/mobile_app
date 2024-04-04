//
//  StopDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDetailsPage: View {
    var stop: Stop
    var route: Route?

    var body: some View {
        Text("Stop: \(stop.name)")
            .navigationTitle("Stop Details")
        Text("Route: \(route?.longName ?? "-")")
    }
}

#Preview {
    StopDetailsPage(
        stop: ObjectCollectionBuilder.Single.shared.stop { $0.name = "Boylston" },
        route: ObjectCollectionBuilder.Single.shared.route { $0.longName = "Green Line B" }
    )
}
