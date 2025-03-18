//
//  RouteTypeExtension.swift
//  iosApp
//
//  Created by Jack Curtis on 10/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

extension RouteType {
    func typeText(isOnly: Bool) -> String {
        // hardcoding plurals because pluralized strings that don't include the number are not supported
        // https://developer.apple.com/forums/thread/737329#737329021
        switch self {
        case .bus:
            isOnly ? NSLocalizedString("bus", comment: "bus") : NSLocalizedString("buses", comment: "buses")

        case .commuterRail, .heavyRail, .lightRail:
            isOnly ? NSLocalizedString("train", comment: "train") : NSLocalizedString("trains", comment: "trains")

        case .ferry: isOnly ? NSLocalizedString("ferry", comment: "ferry")
            : NSLocalizedString("ferries", comment: "ferries")
        }
    }
}
