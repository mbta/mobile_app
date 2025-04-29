//
//  StopDetailsNoTripCard.swift
//  iosApp
//
//  Created by esimon on 1/3/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopDetailsNoTripCard: View {
    var status: UpcomingFormat.NoTripsFormat
    var accentColor: Color
    var routeType: RouteType

    @EnvironmentObject var settingsCache: SettingsCache

    var body: some View {
        StopDetailsIconCard(
            accentColor: accentColor,
            details: detailText,
            header: headerText,
            icon: headerImage
        )
    }

    var detailText: Text? {
        switch onEnum(of: status) {
        case .predictionsUnavailable: Text(settingsCache
                .get(.hideMaps) ? predictionsUnavailableStringNoMap : predictionsUnavailableString)
        default: nil
        }
    }

    @ViewBuilder
    var headerText: some View {
        // Text needed to be copied from UpcomingTripView because that sets the font style,
        // which means we're unable to override to use a larger font if we use that view directly
        switch onEnum(of: status) {
        case .predictionsUnavailable:
            Text("Predictions unavailable")
        case .noSchedulesToday:
            Text("No service today")
        case .serviceEndedToday:
            Text("Service ended")
        }
    }

    var headerImage: Image {
        switch onEnum(of: status) {
        case .predictionsUnavailable: Image(.liveDataSlash)
        case .noSchedulesToday, .serviceEndedToday: routeSlashIcon(routeType)
        }
    }

    var predictionsUnavailableString: String {
        String(format: NSLocalizedString(
            "Service is running, but predicted arrival times aren’t available. Check the map to see where %@ are right now.",
            comment: """
            Explanation under the 'Predictions unavailable' header in stop details when maps are enabled.
            The interpolated value can be "buses" or "trains".
            """
        ), routeType.typeText(isOnly: false))
    }

    var predictionsUnavailableStringNoMap: String {
        NSLocalizedString(
            "Service is running, but predicted arrival times aren’t available.",
            comment: "Explanation under the 'Predictions unavailable' header in stop details."
        )
    }
}
