//
//  RouteCardStopHeader.swift
//  iosApp
//
//  Created by esimon on 4/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteCardStopHeader: View {
    @ObserveInjection var inject
    let data: RouteCardData.RouteStopData
    let globalData: GlobalResponse?

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(data.stop.name)
                .font(Typography.callout)
                .foregroundStyle(Color.text)
            StopAccessibilitySubheader(stop: data.stop, elevatorAlerts: data.elevatorAlerts, globalData: globalData)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.fill2)
        .enableInjection()
    }
}
