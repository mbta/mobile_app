//
//  MoreNavTarget.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-11-01.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

enum MoreNavTarget: Identifiable, Hashable {
    case licenses
    case dependency(Dependency)

    var id: String {
        switch self {
        case .licenses: "licenses"
        case let .dependency(dependency): dependency.id
        }
    }
}
