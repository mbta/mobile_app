//
//  AlertExtension.swift
//  iosApp
//
//  Created by esimon on 6/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared

extension Alert.Cause {
    var causeLowercaseString: String? {
        switch self {
        case .accident: NSLocalizedString("accident", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .amtrak: NSLocalizedString("Amtrak", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .amtrakTrainTraffic: NSLocalizedString(
                "Amtrak train traffic",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .anEarlierMechanicalProblem: NSLocalizedString(
                "an earlier mechanical problem",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .anEarlierSignalProblem:
            NSLocalizedString("an earlier signal problem", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .autosImpedingService: NSLocalizedString(
                "autos impeding service",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .coastGuardRestriction: NSLocalizedString(
                "Coast Guard restriction",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .congestion: NSLocalizedString("congestion", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .construction: NSLocalizedString("construction", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .crossingIssue: NSLocalizedString(
                "crossing issue",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .crossingMalfunction: NSLocalizedString(
                "crossing malfunction",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .demonstration: NSLocalizedString("demonstration", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .disabledBus: NSLocalizedString("disabled bus", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .disabledTrain: NSLocalizedString(
                "disabled train",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .drawbridgeBeingRaised: NSLocalizedString(
                "drawbridge being raised",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .electricalWork: NSLocalizedString(
                "electrical work",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .fire: NSLocalizedString("fire", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .fireDepartmentActivity: NSLocalizedString(
                "fire department activity",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .flooding: NSLocalizedString("flooding", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .fog: NSLocalizedString("fog", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .freightTrainInterference: NSLocalizedString(
                "freight train interference",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .hazmatCondition: NSLocalizedString(
                "hazmat condition",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .heavyRidership: NSLocalizedString(
                "heavy ridership",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .highWinds: NSLocalizedString("high winds", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .holiday: NSLocalizedString("holiday", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .hurricane: NSLocalizedString("hurricane", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .iceInHarbor: NSLocalizedString("ice in harbor", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .maintenance: NSLocalizedString("maintenance", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .mechanicalIssue: NSLocalizedString(
                "mechanical issue",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .mechanicalProblem: NSLocalizedString(
                "mechanical problem",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .medicalEmergency: NSLocalizedString(
                "medical emergency",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .parade: NSLocalizedString("parade", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .policeAction: NSLocalizedString("police action", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .policeActivity: NSLocalizedString(
                "police activity",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .powerProblem: NSLocalizedString("power problem", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .railDefect: NSLocalizedString("rail defect", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .severeWeather: NSLocalizedString(
                "severe weather",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .signalIssue: NSLocalizedString("signal issue", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .signalProblem: NSLocalizedString(
                "signal problem",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .singleTracking: NSLocalizedString(
                "single tracking",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .slipperyRail: NSLocalizedString("slippery rail", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .snow: NSLocalizedString("snow", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .specialEvent: NSLocalizedString("special event", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .speedRestriction: NSLocalizedString(
                "speed restriction",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .strike: NSLocalizedString("strike", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .switchIssue: NSLocalizedString("switch issue", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .switchProblem: NSLocalizedString(
                "switch problem",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .technicalProblem: NSLocalizedString(
                "technical problem",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .tieReplacement: NSLocalizedString(
                "tie replacement",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .trackProblem: NSLocalizedString("track problem", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .trackWork: NSLocalizedString("track work", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .traffic: NSLocalizedString("traffic", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .trainTraffic: NSLocalizedString("train traffic", comment: "Alert cause, used in 'Delays due to [cause]'")
        case .unrulyPassenger: NSLocalizedString(
                "unruly passenger",
                comment: "Alert cause, used in 'Delays due to [cause]'"
            )
        case .weather: NSLocalizedString("weather", comment: "Alert cause, used in 'Delays due to [cause]'")
        default: nil
        }
    }

    var causeString: String? {
        switch self {
        case .accident: NSLocalizedString("Accident", comment: "Possible alert cause")
        case .amtrak: NSLocalizedString("Amtrak", comment: "Possible alert cause")
        case .amtrakTrainTraffic: NSLocalizedString("Amtrak Train Traffic", comment: "Possible alert cause")
        case .anEarlierMechanicalProblem: NSLocalizedString(
                "An Earlier Mechanical Problem",
                comment: "Possible alert cause"
            )
        case .anEarlierSignalProblem: NSLocalizedString("An Earlier Signal Problem", comment: "Possible alert cause")
        case .autosImpedingService: NSLocalizedString("Autos Impeding Service", comment: "Possible alert cause")
        case .coastGuardRestriction: NSLocalizedString("Coast Guard Restriction", comment: "Possible alert cause")
        case .congestion: NSLocalizedString("Congestion", comment: "Possible alert cause")
        case .construction: NSLocalizedString("Construction", comment: "Possible alert cause")
        case .crossingIssue: NSLocalizedString("Crossing Issue", comment: "Possible alert cause")
        case .crossingMalfunction: NSLocalizedString("Crossing Malfunction", comment: "Possible alert cause")
        case .demonstration: NSLocalizedString("Demonstration", comment: "Possible alert cause")
        case .disabledBus: NSLocalizedString("Disabled Bus", comment: "Possible alert cause")
        case .disabledTrain: NSLocalizedString("Disabled Train", comment: "Possible alert cause")
        case .drawbridgeBeingRaised: NSLocalizedString("Drawbridge Being Raised", comment: "Possible alert cause")
        case .electricalWork: NSLocalizedString("Electrical Work", comment: "Possible alert cause")
        case .fire: NSLocalizedString("Fire", comment: "Possible alert cause")
        case .fireDepartmentActivity: NSLocalizedString("Fire Department Activity", comment: "Possible alert cause")
        case .flooding: NSLocalizedString("Flooding", comment: "Possible alert cause")
        case .fog: NSLocalizedString("Fog", comment: "Possible alert cause")
        case .freightTrainInterference: NSLocalizedString("Freight Train Interference", comment: "Possible alert cause")
        case .hazmatCondition: NSLocalizedString("Hazmat Condition", comment: "Possible alert cause")
        case .heavyRidership: NSLocalizedString("Heavy Ridership", comment: "Possible alert cause")
        case .highWinds: NSLocalizedString("High Winds", comment: "Possible alert cause")
        case .holiday: NSLocalizedString("Holiday", comment: "Possible alert cause")
        case .hurricane: NSLocalizedString("Hurricane", comment: "Possible alert cause")
        case .iceInHarbor: NSLocalizedString("Ice In Harbor", comment: "Possible alert cause")
        case .maintenance: NSLocalizedString("Maintenance", comment: "Possible alert cause")
        case .mechanicalIssue: NSLocalizedString("Mechanical Issue", comment: "Possible alert cause")
        case .mechanicalProblem: NSLocalizedString("Mechanical Problem", comment: "Possible alert cause")
        case .medicalEmergency: NSLocalizedString("Medical Emergency", comment: "Possible alert cause")
        case .parade: NSLocalizedString("Parade", comment: "Possible alert cause")
        case .policeAction: NSLocalizedString("Police Action", comment: "Possible alert cause")
        case .policeActivity: NSLocalizedString("Police Activity", comment: "Possible alert cause")
        case .powerProblem: NSLocalizedString("Power Problem", comment: "Possible alert cause")
        case .railDefect: NSLocalizedString("Rail Defect", comment: "Possible alert cause")
        case .severeWeather: NSLocalizedString("Severe Weather", comment: "Possible alert cause")
        case .signalIssue: NSLocalizedString("Signal Issue", comment: "Possible alert cause")
        case .signalProblem: NSLocalizedString("Signal Problem", comment: "Possible alert cause")
        case .singleTracking: NSLocalizedString("Single Tracking", comment: "Possible alert cause")
        case .slipperyRail: NSLocalizedString("Slippery Rail", comment: "Possible alert cause")
        case .snow: NSLocalizedString("Snow", comment: "Possible alert cause")
        case .specialEvent: NSLocalizedString("Special Event", comment: "Possible alert cause")
        case .speedRestriction: NSLocalizedString("Speed Restriction", comment: "Possible alert cause")
        case .strike: NSLocalizedString("Strike", comment: "Possible alert cause")
        case .switchIssue: NSLocalizedString("Switch Issue", comment: "Possible alert cause")
        case .switchProblem: NSLocalizedString("Switch Problem", comment: "Possible alert cause")
        case .technicalProblem: NSLocalizedString("Technical Problem", comment: "Possible alert cause")
        case .tieReplacement: NSLocalizedString("Tie Replacement", comment: "Possible alert cause")
        case .trackProblem: NSLocalizedString("Track Problem", comment: "Possible alert cause")
        case .trackWork: NSLocalizedString("Track Work", comment: "Possible alert cause")
        case .traffic: NSLocalizedString("Traffic", comment: "Possible alert cause")
        case .trainTraffic: NSLocalizedString("Train Traffic", comment: "Possible alert cause")
        case .unrulyPassenger: NSLocalizedString("Unruly Passenger", comment: "Possible alert cause")
        case .weather: NSLocalizedString("Weather", comment: "Possible alert cause")
        default: nil
        }
    }
}

extension Alert.Effect {
    var effectSentenceCaseString: String {
        switch self {
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
    }

    var effectString: String {
        switch self {
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
        case .otherEffect, .unknownEffect: NSLocalizedString("Alert", comment: "Possible alert")
        }
    }
}
