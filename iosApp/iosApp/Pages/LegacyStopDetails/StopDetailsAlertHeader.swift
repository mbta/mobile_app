//
//  StopDetailsAlertHeader.swift
//  iosApp
//
//  Created by Simon, Emma on 7/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct StopDetailsAlertHeader: View {
    let alert: shared.Alert
    let routeColor: Color?

    @ScaledMetric private var iconSize = 16

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            AlertIcon(alertState: alert.alertState, color: routeColor)
                .frame(width: 36, height: 36)
                .padding(6)
            Text(alert.header ?? "")
                .font(.callout)
                .frame(maxWidth: .infinity, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.vertical, 3)
            Image(.faCircleInfo)
                .resizable()
                .frame(width: iconSize, height: iconSize)
                .padding(4)
                .frame(maxHeight: .infinity, alignment: .center)
                .foregroundStyle(Color.deemphasized)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 12)
        .fixedSize(horizontal: false, vertical: true)
    }
}
