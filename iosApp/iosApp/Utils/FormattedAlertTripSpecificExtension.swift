//
//  FormattedAlertTripSpecificExtension.swift
//  iosApp
//
//  Created by esimon on 4/17/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Foundation
import Shared

extension FormattedAlert {
    static func summaryTripIdentity(tripIdentity: TripSpecificAlertSummaryTripIdentity) -> String {
        switch onEnum(of: tripIdentity) {
        case let .thisTrip(tripIdentity): String(
                format: NSLocalizedString(
                    "This %1$@",
                    comment: "Trip identity in the form of ”This [vehicle type]”, ex ”This [train]”"
                ), tripIdentity.routeType.typeText(isOnly: true)
            )
        case let .tripFrom(tripIdentity): String(
                format: NSLocalizedString(
                    "**%1$@** %2$@ from **%3$@**",
                    comment: "Trip identity in the form of ”[time] [vehicle type] from [stop]”, ex “[12:13 PM] [train] from [Ruggles]”"
                ),
                tripIdentity.tripTime.formatted(date: .omitted, time: .shortened),
                tripIdentity.routeType.typeText(isOnly: true),
                tripIdentity.stopName
            )
        case let .tripTo(tripIdentity): String(
                format: NSLocalizedString(
                    "**%1$@** %2$@ to **%3$@**",
                    comment: "Trip identity in the form of ”[time] [vehicle type] to [headsign]”, ex “[12:13 PM] [train] to [Stoughton]”"
                ),
                tripIdentity.tripTime.formatted(date: .omitted, time: .shortened),
                tripIdentity.routeType.typeText(isOnly: true),
                tripIdentity.headsign
            )
        case .multipleTrips: NSLocalizedString(
                "Multiple trips",
                comment: "Trip identity referring to more than one specific trip"
            )
        }
    }

    static func summaryTripEffect(
        tripIdentity: TripSpecificAlertSummaryTripIdentity,
        effect: Alert.Effect,
        effectStops: [String]?,
        isToday: Bool
    ) -> String {
        let day = isToday ? NSLocalizedString("today", comment: "") : NSLocalizedString("tomorrow", comment: "")
        let isPlural = tripIdentity is TripSpecificAlertSummary.MultipleTrips
        switch effect {
        case .cancellation where isPlural: return String(format: NSLocalizedString(
                "are cancelled %@",
                comment: "Multiple trip specific alert effect denoting cancellation, will specify “today” or “tomorrow”"
            ), day)
        case .cancellation: return String(format: NSLocalizedString(
                "is cancelled %@",
                comment: "Trip specific alert effect denoting cancellation, will specify “today” or “tomorrow”"
            ), day)
        case .stationClosure, .stopClosure, .dockClosure: if let effectStops {
                return String(
                    format: NSLocalizedString(
                        "will not stop at %@ %@",
                        comment: "Trip specific alert effect denoting station bypass, ex “will not stop at [Back Bay and Ruggles] [today]”"
                    ),
                    effectStops.map { "**\($0)**" }.reduce(nil) { lhs, rhs in
                        if let lhs { String(
                            format: NSLocalizedString(
                                "%1$@ and %2$@",
                                comment: "Joins two stops into a list, ex “[Back Bay] and [Ruggles]”"
                            ),
                            lhs,
                            rhs
                        ) } else { rhs }
                    } ?? "",
                    day
                )
            }
        case .suspension:
            if let terminatingStop = effectStops?.first {
                return String(format: NSLocalizedString(
                    "will terminate at %1$@ %2$@",
                    comment: "Trip specific alert effect denoting suspension downstream, with interpolated stop name, ex: “will terminate at [Porter]“"
                ), terminatingStop, day)
            } else if isPlural {
                return String(format: NSLocalizedString(
                    "are suspended %@",
                    comment: "Multiple trip specific alert effect denoting suspension, will specify “today” or “tomorrow”"
                ), day)
            } else {
                return String(format: NSLocalizedString(
                    "is suspended %@",
                    comment: "Trip specific alert effect denoting suspension, will specify “today” or “tomorrow”"
                ), day)
            }
        default:
            break
        }
        return String(
            format: NSLocalizedString(
                "affected by %@ %@",
                comment: "Trip specific alert effect fallback, ex “affected by [snow route] [today]”"
            ),
            effect.effectSentenceCaseString,
            day
        )
    }

    func tripSpecificAlertSummary(alertSummary: TripSpecificAlertSummary) -> AttributedString {
        var summaryTripCause =
            if let dueToCause {
                String(format: NSLocalizedString(" due to %@", comment: ""), dueToCause)
            } else {
                ""
            }
        return AttributedString.tryMarkdown(String(
            format: NSLocalizedString(
                "%1$@ %2$@%3$@%4$@",
                comment: """
                Alert summary in the format of “[trip identity] [is affected][due to cause][until recurrence]”, \
                ex “[12:13 PM from Ruggles] [is cancelled today][ due to a mechanical issue][ \
                some days until Wednesday]” or “[Multiple trips] [are suspended today][][]”
                """
            ),
            Self.summaryTripIdentity(tripIdentity: alertSummary.tripIdentity),
            Self.summaryTripEffect(
                tripIdentity: alertSummary.tripIdentity,
                effect: alertSummary.effect,
                effectStops: alertSummary.effectStops,
                isToday: alertSummary.isToday
            ),
            summaryTripCause,
            Self.summaryRecurrence(recurrence: alertSummary.recurrence)
        ))
    }
}
