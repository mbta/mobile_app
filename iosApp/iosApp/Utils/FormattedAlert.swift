//
//  FormattedAlert.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-16.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import shared

struct FormattedAlert: Equatable {
    let effect: String
    let downstreamLabel: String
    /// Represents the text and possible accessibility label that would be used if replacing predictions. Does not
    /// guarantee that the alert should replace predictions.
    let predictionReplacement: PredictionReplacement

    // swiftlint:disable:next cyclomatic_complexity
    init(alert: Alert) {
        effect = switch alert.effect {
        case .accessIssue: NSLocalizedString("Access Issue", comment: "Possible alert effect")
        case .additionalService: NSLocalizedString("Additional Service", comment: "Possible alert effect")
        case .amberAlert: NSLocalizedString("Amber Alert", comment: "Possible alert effect")
        case .bikeIssue: NSLocalizedString("Bike Issue", comment: "Possible alert effect")
        case .cancellation: NSLocalizedString("Cancellation", comment: "Possible alert effect")
        case .delay: NSLocalizedString("Delay", comment: "Possible alert effect")
        case .detour: NSLocalizedString("Detour", comment: "Possible alert effect")
        case .dockClosure: NSLocalizedString("Dock Closure", comment: "Possible alert effect")
        case .dockIssue: NSLocalizedString("Dock Issue", comment: "Possible alert effect")
        case .elevatorClosure: NSLocalizedString("Elevator Closure", comment: "Possible alert effect")
        case .escalatorClosure: NSLocalizedString("Escalator Closure", comment: "Possible alert effect")
        case .extraService: NSLocalizedString("Extra Service", comment: "Possible alert effect")
        case .facilityIssue: NSLocalizedString("Facility Issue", comment: "Possible alert effect")
        case .modifiedService: NSLocalizedString("Modified Service", comment: "Possible alert effect")
        case .noService: NSLocalizedString("No Service", comment: "Possible alert effect")
        case .parkingClosure: NSLocalizedString("Parking Closure", comment: "Possible alert effect")
        case .parkingIssue: NSLocalizedString("Parking Issue", comment: "Possible alert effect")
        case .policyChange: NSLocalizedString("Policy Change", comment: "Possible alert effect")
        case .scheduleChange: NSLocalizedString("Schedule Change", comment: "Possible alert effect")
        case .serviceChange: NSLocalizedString("Service Change", comment: "Possible alert effect")
        case .shuttle: NSLocalizedString("Shuttle", comment: "Possible alert effect")
        case .snowRoute: NSLocalizedString("Snow Route", comment: "Possible alert effect")
        case .stationClosure: NSLocalizedString("Station Closure", comment: "Possible alert effect")
        case .stationIssue: NSLocalizedString("Station Issue", comment: "Possible alert effect")
        case .stopClosure: NSLocalizedString("Stop Closure", comment: "Possible alert effect")
        case .stopMove, .stopMoved: NSLocalizedString("Stop Moved", comment: "Possible alert effect")
        case .stopShoveling: NSLocalizedString("Stop Shoveling", comment: "Possible alert effect")
        case .summary: NSLocalizedString("Summary", comment: "Possible alert effect")
        case .suspension: NSLocalizedString("Suspension", comment: "Possible alert effect")
        case .trackChange: NSLocalizedString("Track Change", comment: "Possible alert effect")
        case .otherEffect, .unknownEffect: NSLocalizedString("Alert", comment: "Possible alert effect")
        }
        let downstreamEffect = switch alert.effect {
        case .accessIssue: NSLocalizedString("Access issue", comment: "Possible alert effect")
        case .additionalService: NSLocalizedString("Additional service", comment: "Possible alert effect")
        case .amberAlert: NSLocalizedString("Amber alert", comment: "Possible alert effect")
        case .bikeIssue: NSLocalizedString("Bike issue", comment: "Possible alert effect")
        case .cancellation: NSLocalizedString("Trip cancelled", comment: "Possible alert effect")
        case .delay: NSLocalizedString("Delay", comment: "Possible alert effect")
        case .detour: NSLocalizedString("Detour", comment: "Possible alert effect")
        case .dockClosure: NSLocalizedString("Dock closed", comment: "Possible alert effect")
        case .dockIssue: NSLocalizedString("Dock issue", comment: "Possible alert effect")
        case .elevatorClosure: NSLocalizedString("Elevator closed", comment: "Possible alert effect")
        case .escalatorClosure: NSLocalizedString("Escalator closed", comment: "Possible alert effect")
        case .extraService: NSLocalizedString("Extra service", comment: "Possible alert effect")
        case .facilityIssue: NSLocalizedString("Facility issue", comment: "Possible alert effect")
        case .modifiedService: NSLocalizedString("Modified service", comment: "Possible alert effect")
        case .noService: NSLocalizedString("No service", comment: "Possible alert effect")
        case .parkingClosure: NSLocalizedString("Parking closed", comment: "Possible alert effect")
        case .parkingIssue: NSLocalizedString("Parking issue", comment: "Possible alert effect")
        case .policyChange: NSLocalizedString("Policy change", comment: "Possible alert effect")
        case .scheduleChange: NSLocalizedString("Schedule change", comment: "Possible alert effect")
        case .serviceChange: NSLocalizedString("Service change", comment: "Possible alert effect")
        case .shuttle: NSLocalizedString("Shuttle buses", comment: "Possible alert effect")
        case .snowRoute: NSLocalizedString("Snow route", comment: "Possible alert effect")
        case .stationClosure: NSLocalizedString("Station closed", comment: "Possible alert effect")
        case .stationIssue: NSLocalizedString("Station issue", comment: "Possible alert effect")
        case .stopClosure: NSLocalizedString("Stop closed", comment: "Possible alert effect")
        case .stopMove, .stopMoved: NSLocalizedString("Stop moved", comment: "Possible alert effect")
        case .stopShoveling: NSLocalizedString("Stop shoveling", comment: "Possible alert effect")
        case .summary: NSLocalizedString("Summary", comment: "Possible alert effect")
        case .suspension: NSLocalizedString("Service suspended", comment: "Possible alert effect")
        case .trackChange: NSLocalizedString("Track change", comment: "Possible alert effect")
        case .otherEffect, .unknownEffect: NSLocalizedString("Alert", comment: "Possible alert effect")
        }
        downstreamLabel = String(format: NSLocalizedString("%@ ahead", comment: """
        Label for an alert that exists on a future stop along the selected route,
        the interpolated value can be any alert effect,
        ex. "[Detour] ahead", "[Shuttle buses] ahead"
        """), downstreamEffect)
        // a handful of cases have different text when replacing predictions than in a details title
        predictionReplacement = switch alert.effect {
        case .dockClosure: .init(text: NSLocalizedString("Dock Closed", comment: "Possible alert effect"))
        case .shuttle: .init(
                text: NSLocalizedString("Shuttle Bus", comment: "Possible alert effect"),
                accessibilityLabel: NSLocalizedString(
                    "Shuttle buses replace service",
                    comment: "Shuttle alert VoiceOver text"
                )
            )
        case .stationClosure: .init(text: NSLocalizedString("Station Closed", comment: "Possible alert effect"))
        case .stopClosure: .init(text: NSLocalizedString("Stop Closed", comment: "Possible alert effect"))
        case .suspension: .init(
                text: NSLocalizedString("Suspension", comment: "Possible alert effect"),
                accessibilityLabel: NSLocalizedString("Service suspended", comment: "Suspension alert VoiceOver text")
            )
        default: .init(text: effect, accessibilityLabel: nil)
        }
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
