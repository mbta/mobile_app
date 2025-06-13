//
//  RoutePillSpecContentDescriptionExtension.swift
//  iosApp
//
//  Created by Melody Horn on 6/9/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

extension RoutePillSpec.ContentDescription {
    var text: String {
        switch onEnum(of: self) {
        case let .stopSearchResultRoute(description): if let routeName = description.routeName {
                "\(routeName) \(description.routeType.typeText(isOnly: description.isOnly))"
            } else {
                description.routeType.typeText(isOnly: description.isOnly)
            }
        }
    }
}
