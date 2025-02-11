//
//  AlertExtension.swift
//  iosApp
//
//  Created by Kayla Brady on 11/14/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

extension shared.Alert {
    func downstreamEffectDescription() -> String {
        let effect = switch self.effect {
        case .detour: NSLocalizedString("Detour", comment: "Possible alert effect")
        case .shuttle: NSLocalizedString("Shuttle buses", comment: "Possible alert effect")
        case .snowRoute: NSLocalizedString("Snow route", comment: "Possible alert effect")
        case .stopClosure: NSLocalizedString("Stop closed", comment: "Possible alert effect")
        case .suspension: NSLocalizedString("Service suspended", comment: "Possible alert effect")
        case .serviceChange: NSLocalizedString("Service change", comment: "Possible alert effect")
        default: NSLocalizedString("Alert", comment: "Possible alert effect")
        }
        return String(format: NSLocalizedString("%@ ahead", comment: """
        Label for an alert that exists on a future stop along the selected route,
        the interpolated value can be any alert effect,
        ex. "[Detour] ahead", "[Shuttle buses] ahead"
        """), effect)
    }
}
