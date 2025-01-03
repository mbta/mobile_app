//
//  StopDetailsNoTripCard.swift
//  iosApp
//
//  Created by esimon on 1/3/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct StopDetailsNoTripCard: View {
    var status: RealtimePatterns.Format
    var headerColor: Color
    var routeType: RouteType

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 16) {
                headerImage.foregroundStyle(headerColor).frame(width: 48, height: 48)
                headerText.font(Typography.title2Bold).foregroundStyle(headerColor)
            }.frame(maxWidth: .infinity, alignment: .leading)

            if let details = detailString {
                HaloSeparator()
                Text(details).font(Typography.callout)
            }
        }
        .padding(16)
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 1))
        .padding(.horizontal, 16)
    }

    var detailString: String? {
        switch onEnum(of: status) {
        case .none: String(format: NSLocalizedString(
                "Service is running, but predicted arrival times aren’t available. The map shows where %@ on this route currently are.",
                comment: """
                Explanation under the 'Predictions unavailable' header in stop details.
                The interpolated value can be "buses" or "trains".
                """
            ), routeType.typeText(isOnly: false))
        default: nil
        }
    }

    @ViewBuilder
    var headerText: some View {
        // Text needed to be copied from UpcomingTripView because that sets the font style,
        // which means we're unable to override to use a larger font if we use that view directly
        switch onEnum(of: status) {
        case .none:
            Text("Predictions unavailable")
        case .noSchedulesToday:
            Text("No service today")
        case .serviceEndedToday:
            Text("Service ended")
        default: EmptyView()
        }
    }

    @ViewBuilder
    var headerImage: some View {
        switch onEnum(of: status) {
        case .none: Image(.liveDataSlash)
            .resizable().scaledToFit().frame(width: 35, height: 35)
        case .noSchedulesToday, .serviceEndedToday: modeSlashImage
            .resizable().scaledToFit().frame(width: 35, height: 35)
        default: EmptyView()
        }
    }

    var modeSlashImage: Image {
        switch routeType {
        case .bus: Image(.modeBusSlash)
        case .commuterRail: Image(.modeCrSlash)
        case .ferry: Image(.modeFerrySlash)
        default: Image(.modeSubwaySlash)
        }
    }
}
