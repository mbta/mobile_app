//
//  TextExtension.swift
//  iosApp
//
//  Created by Melody Horn on 7/28/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

extension Text {
    enum EasternTimeInstantStyle {
        case time
    }

    init(_ instant: EasternTimeInstant, style: EasternTimeInstantStyle) {
        let text = switch style {
        case .time: instant.formatted(date: .omitted, time: .shortened)
        }
        self.init(verbatim: text)
    }
}
