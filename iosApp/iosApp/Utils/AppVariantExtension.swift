//
//  AppVariantExtension.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-08-21.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

extension AppVariant {
    func styleUri(colorScheme: ColorScheme) -> StyleURI {
        if colorScheme == .light {
            .init(rawValue: lightMapStyle) ?? .light
        } else {
            .init(rawValue: darkMapStyle) ?? .dark
        }
    }
}
