//
//  MoreItem.swift
//  iosApp
//
//  Created by esimon on 10/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

enum MoreItem: Identifiable, Equatable {
    case toggle(setting: Setting)
    case link(label: String, url: String, note: String? = nil)
    case phone(label: String, phoneNumber: String)

    var id: String {
        switch self {
        case let .toggle(setting: setting): setting.key.name
        case let .link(label: _, url: url, note: _): url
        case let .phone(label: _, phoneNumber: number): number
        }
    }

    var label: String {
        switch self {
        case let .toggle(setting: setting):
            switch setting.key {
            case .hideMaps: NSLocalizedString(
                    "Hide Maps",
                    comment: "A setting on the More page to remove the app component from the app"
                )
            case .searchRouteResults: NSLocalizedString(
                    "Route Search",
                    comment: "A setting on the More page to display routes in search (only visible for developers)"
                )
            case .map: NSLocalizedString(
                    "Map Debug",
                    comment: "A setting on the More page to display map debug information (only visible for developers)"
                )
            }

        case let .link(label: label, url: _, note: _): label

        case let .phone(label: label, phoneNumber: _): label
        }
    }
}
