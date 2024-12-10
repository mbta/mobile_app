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
    routeIcon(route.type)
}

func routeIcon(_ routeType: RouteType) -> Image {
    Image(routeIconResource(routeType))
}

func routeIconResource(_ routeType: RouteType) -> ImageResource {
    switch routeType {
    case .bus: .modeBus
    case .commuterRail: .modeCr
    case .ferry: .modeFerry
    default: .modeSubway
    }
}
