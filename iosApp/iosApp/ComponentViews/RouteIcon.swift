//
//  RouteIcon.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

func routeIcon(_ route: Route) -> Image {
    switch route.type {
    case .bus:
        Image(.modeBus)
    case .commuterRail:
        Image(.modeCr)
    case .ferry:
        Image(.modeFerry)
    default:
        Image(.modeSubway)
    }
}
