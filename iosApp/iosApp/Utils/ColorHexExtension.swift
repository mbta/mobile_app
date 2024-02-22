//
//  ColorHexExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 2/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import SwiftUI

extension Color {
    init(hex: String) {
        var cString: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

        if cString.hasPrefix("#") {
            cString.remove(at: cString.startIndex)
        }

        if (cString.count) != 6 {
            self.init(.gray)
            Logger().error("Failed to parse provided hex string '\(cString)', it was too long")
            return
        }

        let gIndex = cString.index(cString.startIndex, offsetBy: 2)
        let bIndex = cString.index(cString.startIndex, offsetBy: 4)

        let rInt = UInt64(String(cString[cString.startIndex ..< gIndex]), radix: 16)
        let gInt = UInt64(String(cString[gIndex ..< bIndex]), radix: 16)
        let bInt = UInt64(String(cString[bIndex ..< cString.endIndex]), radix: 16)

        if rInt == nil || gInt == nil || bInt == nil {
            self.init(.gray)
            Logger().error("Failed to parse provided hex string '\(cString)', value was not a hex number")
            return
        }

        self.init(
            red: CGFloat(rInt!) / 255.0,
            green: CGFloat(gInt!) / 255.0,
            blue: CGFloat(bInt!) / 255.0
        )
    }
}
