//
//  TrackNumber.swift
//  iosApp
//
//  Created by esimon on 10/8/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct TrackNumber: View {
    @ObserveInjection var inject
    var track: TripDetailsStopList.Track

    var trackText: Text {
        switch onEnum(of: track) {
        case let .number(number): Text("Track \(number.number)")
        case .tBD: Text("Track TBD")
        }
    }

    var trackLabel: Text {
        switch onEnum(of: track) {
        case let .number(number): Text("Boarding on track \(number.number)")
        case .tBD: Text("Track to be determined")
        }
    }

    var body: some View {
        trackText
            .font(Typography.footnote)
            .foregroundStyle(Color.text)
            .multilineTextAlignment(.leading)
            .accessibilityLabel(trackLabel)
            .enableInjection()
    }
}
