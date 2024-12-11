//
//  RouteExtension.swift
//  iosApp
//
//  Created by esimon on 12/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

extension Route {
    var uiColor: Color { Color(hex: color) }
    var uiTextColor: Color { Color(hex: textColor) }
}
