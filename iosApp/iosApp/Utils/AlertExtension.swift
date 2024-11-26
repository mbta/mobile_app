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
    func effectDescription() -> Text {
        switch effect {
        case .detour: Text("Detour", comment: "Possible alert effect")
        case .shuttle: Text("Shuttle buses", comment: "Possible alert effect")
        case .snowRoute: Text("Snow route", comment: "Possible alert effect")
        case .stopClosure: Text("Stop closed", comment: "Possible alert effect")
        case .suspension: Text("Service suspended", comment: "Possible alert effect")
        default: Text("Alert", comment: "Possible alert effect")
        }
    }
}
