//
//  FormattedAlertTripShuttleExtension.swift
//  iosApp
//
//  Created by esimon on 4/17/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation
import Shared

extension FormattedAlert {
    static func summaryTripShuttleIdentity(tripIdentity: TripShuttleAlertSummaryTripIdentity) -> String {
        switch onEnum(of: tripIdentity) {
        case let .singleTrip(tripIdentity):
            if let fromStop = tripIdentity.fromStopName {
                String(
                    format: NSLocalizedString(
                        "**%1$@** %2$@ from **%3$@**",
                        comment: "Trip identity in the format of “[time] [vehicle] from [stop]”, ex “[12:13 PM] [train] from [South Station]“"
                    ),
                    tripIdentity.tripTime.formatted(date: .omitted, time: .shortened),
                    tripIdentity.routeType.typeText(isOnly: true),
                    fromStop
                )
            } else {
                String(
                    format: NSLocalizedString(
                        "the **%@** %@",
                        comment: "Trip identity in the format of “the [time] [vehicle]”, ex “the [12:13 PM] [train]"
                    ),
                    tripIdentity.tripTime.formatted(date: .omitted, time: .shortened),
                    tripIdentity.routeType.typeText(isOnly: true)
                )
            }

        case let .thisTrip(tripIdentity): String(
                format: NSLocalizedString(
                    "this %1$@",
                    comment: "Trip identity in the form of ”this [vehicle type]”, ex ”this [train]”"
                ), tripIdentity.routeType.typeText(isOnly: true)
            )

        case .multipleTrips: NSLocalizedString(
                "multiple trips",
                comment: "Trip identity referring to more than one specific trip"
            )
        }
    }

    func tripShuttleAlertSummary(alertSummary: TripShuttleAlertSummary) -> AttributedString {
        let identity = alertSummary.tripIdentity
        let identityString = Self.summaryTripShuttleIdentity(tripIdentity: identity)

        return if case let .singleTrip(singleTrip) = onEnum(of: identity), singleTrip.fromStopName != nil {
            AttributedString.tryMarkdown(String(
                format: NSLocalizedString(
                    "%1$@ is replaced by shuttle buses from **%2$@** to **%3$@**%4$@",
                    comment: """
                    Alert summary in the format of “[trip identity] is replaced by shuttle buses \
                    from [stop] to [stop][until recurrence]”, ex “Shuttle buses replace [the 12:13 PM train] \
                    from [Ruggles] to [Forest Hills][ some days until Friday]”
                    """
                ),
                identityString,
                alertSummary.startStopName,
                alertSummary.endStopName,
                Self.summaryRecurrence(recurrence: alertSummary.recurrence)
            ))
        } else {
            AttributedString.tryMarkdown(String(
                format: NSLocalizedString(
                    "Shuttle buses replace %1$@ from **%2$@** to **%3$@**%4$@",
                    comment: """
                    Alert summary in the format of “Shuttle buses replace [trip identity] \
                    from [stop] to [stop][until recurrence]”, ex “Shuttle buses replace [this train] \
                    from [Ruggles] to [Forest Hills][ some days until Friday]”
                    """
                ),
                identityString,
                alertSummary.startStopName,
                alertSummary.endStopName,
                Self.summaryRecurrence(recurrence: alertSummary.recurrence)
            ))
        }
    }
}
