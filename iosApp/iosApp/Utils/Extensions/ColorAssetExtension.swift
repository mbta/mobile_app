//
//  ColorAssetExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/7/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

extension Color {
    static let accessibility = Color("Accessibility")
    static let contrast = Color("Contrast")
    static let deemphasized = Color("Deemphasized")
    static let delete = Color("Delete")
    static let deleteBackground = Color("Delete Background")
    static let deselectedToggle2 = Color("Deselected Toggle 2")
    static let deselectedToggleText = Color("Deselected Toggle Text")
    static let error = Color("Error")
    static let fill1 = Color("Fill 1")
    static let fill2 = Color("Fill 2")
    static let fill3 = Color("Fill 3")
    static let halo = Color("Halo")
    static let haloContrast = Color("Halo Contrast")
    static let haloLight = Color("Halo Light")
    static let haloDark = Color("Halo Dark")
    static let key = Color("Key")
    static let keyInverse = Color("Key Inverse")
    static let routeColorContrast = Color("Route Color Contrast")
    static let routeColorContrastText = Color("Route Color Contrast Text")
    static let sheetBackground = Color("Sheet Background")
    static let stopDotHalo = Color("Stop Dot Halo")
    static let text = Color("Text")
    static let textContrast = Color("Text Contrast")

    static let translucentContrast = text.opacity(0.6)
}

extension UIColor {
    // The built in color scheme switching doesn't work with `Color` in layer styles,
    // using name strings in UIColor does pick up theme changes properly though.
    // The named colors should always exist unless they're removed from Colors.xcassets,
    // but if they're removed, the fallback using Color will not be responsive to theme.
    static let accessibility = UIColor(named: "Accessibility") ?? UIColor(Color(.accessibility))
    static let contrast = UIColor(named: "Contrast") ?? UIColor(Color(.contrast))
    static let deemphasized = UIColor(named: "Deemphasized") ?? UIColor(Color(.deemphasized))
    static let delete = UIColor(named: "Delete") ?? UIColor(Color(.delete))
    static let deleteBackground = UIColor(named: "Delete Background") ?? UIColor(Color(.deleteBackground))
    static let deselectedToggle2 = UIColor(named: "Deselected Toggle 2") ?? UIColor(Color(.deselectedToggle2))
    static let deselectedToggleText = UIColor(named: "Deselected Toggle Text") ?? UIColor(Color(.deselectedToggleText))
    static let error = UIColor(named: "Error") ?? UIColor(Color(.error))
    static let fill1 = UIColor(named: "Fill 1") ?? UIColor(Color(.fill1))
    static let fill2 = UIColor(named: "Fill 2") ?? UIColor(Color(.fill2))
    static let fill3 = UIColor(named: "Fill 3") ?? UIColor(Color(.fill3))
    static let halo = UIColor(named: "Halo") ?? UIColor(Color(.halo))
    static let key = UIColor(named: "Key") ?? UIColor(Color(.key))
    static let keyInverse = UIColor(named: "Key Inverse") ?? UIColor(Color(.keyInverse))
    static let routeColorContrast = UIColor(named: "Route Color Contrast") ?? UIColor(Color(.routeColorContrast))
    static let routeColorContrastText = UIColor(named: "Route Color Contrast Text") ??
        UIColor(Color(.routeColorContrastText))
    static let sheetBackground = UIColor(named: "Sheet Background") ?? UIColor(Color(.sheetBackground))
    static let stopDotHalo = UIColor(named: "Stop Dot Halo") ?? UIColor(Color(.stopDotHalo))
    static let text = UIColor(named: "Text") ?? UIColor(Color(.text))

    static let translucentContrast = text.withAlphaComponent(0.6)
}
