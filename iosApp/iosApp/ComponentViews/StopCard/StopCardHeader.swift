//
//  StopCardHeader.swift
//  iosApp
//
//  Created by Melody Horn on 6/25/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopCardHeaderWithAccessibility: View {
    @ObserveInjection var inject
    let data: StopCardData
    let globalData: GlobalResponse?

    var body: some View {
        StopCardHeader(stop: data.stop) {
            StopAccessibilitySubheader(stop: data.stop, elevatorAlerts: data.elevatorAlerts, globalData: globalData)
        }
    }
}

struct StopCardHeader<Subheader: View>: View {
    let stop: Stop
    @ViewBuilder let subheader: Subheader

    var body: some View {
        HStack(spacing: 4) {
            (stop.locationType == .stop ? Image(.mapStopCloseBUS) : Image(.mbtaLogo))
                .resizable()
                .scaledToFit()
                .frame(width: 24, height: 24)
                .accessibilityHidden(true)
                .padding(.horizontal, 8)
            VStack(alignment: .leading, spacing: 4) {
                Text(stop.name)
                    .font(Typography.bodySemibold)
                    .foregroundStyle(Color.text)

                subheader
            }
            .padding(.trailing, 16)
            .padding(.vertical, 12)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.halo)
        .enableInjection()
    }
}
