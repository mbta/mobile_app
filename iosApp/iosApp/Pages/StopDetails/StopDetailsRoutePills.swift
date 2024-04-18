//
//  StopDetailsRoutePills.swift
//  iosApp
//
//  Created by Simon, Emma on 4/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct StopDetailsRoutePills: View {
    let servedRoutes: [Route]
    let tapRoutePill: (Route) -> Void
    @Binding var filter: StopDetailsFilter?

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack {
                ForEach(servedRoutes, id: \.id) { route in
                    RoutePill(route: route, isActive: filter == nil || filter?.routeId == route.id)
                        .onTapGesture { tapRoutePill(route) }
                }
            }
            .padding(.horizontal, 15)
        }
    }
}
