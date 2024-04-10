//
//  SheetNavigationStackEntry.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-04.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

struct StopDetailsFilter: Hashable {
    let routeId: String
    let directionId: Int32
}

enum SheetNavigationStackEntry: Hashable {
    case stopDetails(Stop, StopDetailsFilter?)
}
