//
//  StopDetailsDownstreamAlert.swift
//  iosApp
//
//  Created by Kayla Brady on 11/14/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct StopDetailsDownstreamAlert: View {
    let alert: shared.Alert
    let routeColor: Color?

    @ScaledMetric private var iconSize = 16

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            AlertIcon(alertState: .issue, color: routeColor)
                .frame(width: 24, height: 24)
                .padding(6)
            Text(FormattedAlert(alert: alert).downstreamLabel)
                .font(.callout)
                .frame(maxWidth: .infinity, alignment: .leading)
            InfoIcon(size: iconSize).padding(4)
        }
        .padding(.horizontal, 8).padding(.vertical, 10)
        .background(Color.fill2)
    }
}
