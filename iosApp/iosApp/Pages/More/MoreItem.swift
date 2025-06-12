//
//  MoreItem.swift
//  iosApp
//
//  Created by esimon on 10/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import Shared

enum MoreItem: Identifiable, Equatable {
    case toggle(label: String, setting: Settings)
    case link(label: String, url: String, note: String? = nil)
    case phone(label: String, phoneNumber: String)
    case navLink(label: String, destination: MoreNavTarget)

    var id: String {
        switch self {
        case let .toggle(label: _, setting: setting): setting.name
        case let .link(label: _, url: url, note: _): url
        case let .phone(label: _, phoneNumber: number): number
        case let .navLink(label: _, destination: destination): destination.id
        }
    }
}
