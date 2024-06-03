//
//  ColorAssetExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/7/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

extension Color {
    static let deemphasized = Color("Deemphasized")
    static let fill1 = Color("Fill 1")
    static let fill2 = Color("Fill 2")
    static let fill3 = Color("Fill 3")
    static let halo = Color("Halo")
    static let key = Color("Key")
    static let text = Color("Text")
}

extension UIColor {
    // The built in color scheme switching doesn't work with `Color` in layer styles,
    // using name strings in UIColor does pick up theme changes properly though.
    // The named colors should always exist unless they're removed from Colors.xcassets,
    // but if they're removed, the fallback using Color will not be responsive to theme.
    static let deemphasized = UIColor(named: "Deemphasized") ?? UIColor(Color(.deemphasized))
    static let fill1 = UIColor(named: "Fill 1") ?? UIColor(Color(.fill1))
    static let fill2 = UIColor(named: "Fill 2") ?? UIColor(Color(.fill2))
    static let fill3 = UIColor(named: "Fill 3") ?? UIColor(Color(.fill3))
    static let halo = UIColor(named: "Halo") ?? UIColor(Color(.halo))
    static let key = UIColor(named: "Key") ?? UIColor(Color(.key))
    static let text = UIColor(named: "Text") ?? UIColor(Color(.text))
}
