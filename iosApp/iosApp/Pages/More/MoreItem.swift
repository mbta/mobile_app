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
    case toggle(label: String, setting: Settings, value: Bool)
    case link(label: String, url: String, note: String? = nil)
    case phone(label: String, phoneNumber: String)

    var id: String {
        switch self {
        case let .toggle(label: _, setting: setting, value: _): setting.name
        case let .link(label: _, url: url, note: _): url
        case let .phone(label: _, phoneNumber: number): number
        }
    }
}
