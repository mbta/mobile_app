//
//  FormattedAlert.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-16.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import RegexBuilder
import Shared

// swiftlint:disable:next type_body_length
struct FormattedAlert: Equatable {
    let alert: Alert
    let alertSummaryEntity: AlertSummaryEntity?
    let effect: String
    let sentenceCaseEffect: String
    let dueToCause: String?
    /// Represents the text and possible accessibility label that would be used if replacing predictions. Does not
    /// guarantee that the alert should replace predictions.
    let predictionReplacement: PredictionReplacement

    init(alert: Alert, alertSummaryEntity: AlertSummaryEntity? = nil) {
        self.alert = alert
        let effect = alert.effect
        self.effect = "**\(effect.effectString)**"
        sentenceCaseEffect = effect.effectSentenceCaseString
        let cause = alert.cause
        dueToCause = cause?.causeLowercaseString

        // a handful of cases have different text when replacing predictions than in a details title
        predictionReplacement = switch effect {
        case .dockClosure: .init(text: NSLocalizedString("Dock Closed", comment: "Possible alert effect"))
        case .shuttle: .init(
                text: NSLocalizedString("Shuttle Bus", comment: "Possible alert effect"),
                accessibilityLabel: NSLocalizedString("Shuttle buses replace service",
                                                      comment: "Shuttle alert VoiceOver text")
            )
        case .stationClosure: .init(text: NSLocalizedString("Station Closed", comment: "Possible alert effect"))
        case .stopClosure: .init(text: NSLocalizedString("Stop Closed", comment: "Possible alert effect"))
        case .suspension: .init(
                text: NSLocalizedString("Suspension", comment: "Possible alert effect"),
                accessibilityLabel: NSLocalizedString("Service suspended", comment: "Suspension alert VoiceOver text")
            )
        default: .init(text: effect.effectString, accessibilityLabel: nil)
        }
        self.alertSummaryEntity = alertSummaryEntity
    }

    var downstreamLabel: String {
        String(format: NSLocalizedString("**%@** ahead", comment: """
        Label for an alert that exists on a future stop along the selected route,
        the interpolated value can be any alert effect,
        ex. "[Detour] ahead", "[Shuttle buses] ahead"
        """), sentenceCaseEffect)
    }

    var delaysDueToCause: String {
        if let cause = dueToCause {
            String(format: NSLocalizedString(
                "**Delays** due to %@",
                comment: "Describes the cause of a delay. Ex: 'Delays due to [traffic]'"
            ), cause)
        } else {
            NSLocalizedString("**Delays**", comment: "Generic delay alert label when cause is unknown")
        }
    }

    var summary: AttributedString? {
        if let summary = alertSummaryEntity?.summary {
            AttributedString.tryMarkdown(summary)
        } else {
            nil
        }
    }

    func delayHeader(now: EasternTimeInstant) -> AttributedString {
        let startingLaterToday = alert.currentPeriod(time: now) == nil &&
        alert.nextPeriod(time: now, within: .init(days: 1))?.startServiceDate == now.serviceDate
        if startingLaterToday, let summary {
            return summary
        }
        // Show "Single Tracking" if there is an informational delay alert with that cause
        // (Any other information severity delay alerts are never shown)
        guard let cause = alert.cause?.causeString,
              alert.cause == .singleTracking,
              alert.severity < 3
        else {
            return AttributedString.tryMarkdown(delaysDueToCause)
        }
        return AttributedString.tryMarkdown("**\(cause)**")
    }

    var elevatorHeader: AttributedString {
        let facilities = alert.informedEntity.compactMap { entity in
            if let facilityId = entity.facility { alert.facilities?[facilityId] } else { nil }
        }.filter { $0.type == .elevator }
        let headerString =
            if let facility = Set(facilities).count == 1 ? facilities.first : nil,
            let facilityName = facility.shortName {
                String(format:
                    NSLocalizedString(
                        "Elevator closure (%1$@)",
                        comment: """
                        Alert header for elevator closure alerts, \
                        the interpolated value is the short name field of the elevator facility, \
                        ex "Elevator closure (Red Line platforms to lobby)"
                        """
                    ), facilityName)
            } else if let header = alert.header {
                header
            } else {
                effect
            }
        return AttributedString.tryMarkdown(headerString)
    }

    func alertCardHeader(spec: AlertCardSpec, type: RouteType, now: EasternTimeInstant) -> AttributedString {
        let isTripSpecific = alert.informedEntity.contains(where: { $0.trip != nil })
        return switch spec {
        case .delay: delayHeader(now: now)
        case .downstream: summary ?? AttributedString.tryMarkdown(downstreamLabel)
        case .elevator: elevatorHeader
        case .basic: summary ?? AttributedString.tryMarkdown(effect)
        default: switch (type, alert.effect) {
            case (.bus, .cancellation) where isTripSpecific:
                AttributedString(NSLocalizedString("Bus cancelled", comment: ""))
            case (.ferry, .cancellation) where isTripSpecific:
                AttributedString(NSLocalizedString("Ferry cancelled", comment: ""))
            case (_, .cancellation) where isTripSpecific:
                AttributedString(NSLocalizedString("Train cancelled", comment: ""))
            case (_, .shuttle) where isTripSpecific:
                AttributedString(NSLocalizedString("Shuttle bus", comment: ""))
            case (.bus, .suspension) where isTripSpecific:
                AttributedString(NSLocalizedString("Bus suspended", comment: ""))
            case (.ferry, .suspension) where isTripSpecific:
                AttributedString(NSLocalizedString("Ferry suspended", comment: ""))
            case (_, .suspension) where isTripSpecific:
                AttributedString(NSLocalizedString("Train suspended", comment: ""))
            case (_, .stationClosure) where isTripSpecific,
                 (_, .stopClosure) where isTripSpecific,
                 (_, .dockClosure) where isTripSpecific:
                AttributedString(NSLocalizedString("Stop skipped", comment: ""))
            default: AttributedString.tryMarkdown(effect)
            }
        }
    }

    var alertCardMajorBody: AttributedString {
        summary ?? AttributedString(alert.header ?? "")
    }

    struct PredictionReplacement: Equatable {
        let text: String
        let accessibilityLabel: String?

        init(text: String, accessibilityLabel: String? = nil) {
            self.text = text
            self.accessibilityLabel = accessibilityLabel
        }
    }
}
