//
//  FormattedAlert.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-16.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import shared

struct FormattedAlert {
    let effect: String

    init?(alert: Alert) {
        // TODO: Add all possible alert effects if/when we start displaying non-disruption alerts here
        guard let effect = switch alert.effect {
        case .detour: NSLocalizedString("Detour", comment: "Possible alert effect")
        case .dockClosure: NSLocalizedString("Dock Closure", comment: "Possible alert effect")
        case .elevatorClosure: NSLocalizedString("Elevator Closure", comment: "Possible alert effect")
        case .serviceChange: NSLocalizedString("Service Change", comment: "Possible alert effect")
        case .shuttle: NSLocalizedString("Shuttle", comment: "Possible alert effect")
        case .stationClosure: NSLocalizedString("Station Closure", comment: "Possible alert effect")
        case .stopClosure: NSLocalizedString("Stop Closure", comment: "Possible alert effect")
        case .stopMove, .stopMoved: NSLocalizedString("Station Moved", comment: "Possible alert effect")
        case .suspension: NSLocalizedString("Suspension", comment: "Possible alert effect")
        case .trackChange: NSLocalizedString("Track Change", comment: "Possible alert effect")
        default: nil
        } else { return nil }
        self.effect = effect
    }
}
