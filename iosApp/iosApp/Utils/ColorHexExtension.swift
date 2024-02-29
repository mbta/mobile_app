//
//  ColorHexExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 2/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import SwiftUI

struct Rgb {
    let red: CGFloat
    let green: CGFloat
    let blue: CGFloat
}

func getRgb(hex: String) -> Rgb {
    var cString: String = hex.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()

    if cString.hasPrefix("#") {
        cString.remove(at: cString.startIndex)
    }

    if (cString.count) != 6 {
        Logger().error("Failed to parse provided hex string '\(cString)', it was too long")
        return Rgb(red: 0.0, green: 0.0, blue: 0.0)
    }

    let gIndex = cString.index(cString.startIndex, offsetBy: 2)
    let bIndex = cString.index(cString.startIndex, offsetBy: 4)

    let rInt = UInt64(String(cString[cString.startIndex ..< gIndex]), radix: 16)
    let gInt = UInt64(String(cString[gIndex ..< bIndex]), radix: 16)
    let bInt = UInt64(String(cString[bIndex ..< cString.endIndex]), radix: 16)

    if rInt == nil || gInt == nil || bInt == nil {
        Logger().error("Failed to parse provided hex string '\(cString)', value was not a hex number")
        return Rgb(red: 0.0, green: 0.0, blue: 0.0)
    }

    return Rgb(red: CGFloat(rInt!), green: CGFloat(gInt!), blue: CGFloat(bInt!))
}

extension Color {
    init(hex: String) {
        let rgb = getRgb(hex: hex)
        self.init(red: rgb.red / 255.0, green: rgb.green / 255.0, blue: rgb.blue / 255.0)
    }
}

extension UIColor {
    convenience init(hex: String, alpha: CGFloat = 1.0) {
        let rgb = getRgb(hex: hex)
        self.init(red: rgb.red / 255.0, green: rgb.green / 255.0, blue: rgb.blue / 255.0, alpha: alpha)
    }
}
