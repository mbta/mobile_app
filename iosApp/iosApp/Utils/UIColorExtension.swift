//
//  UIColorExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/31/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

extension UIColor {
    var light: UIColor {
        resolvedColor(with: .init(userInterfaceStyle: .light))
    }

    var dark: UIColor {
        resolvedColor(with: .init(userInterfaceStyle: .dark))
    }
}
