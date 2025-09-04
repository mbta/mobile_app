//
//  PresentationDetentExtension.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 3/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI
import UIKit

extension PresentationDetent {
    static let mediumDetentFraction = 0.56

    static let small = Self.height(150)
    static let medium = Self.fraction(mediumDetentFraction)
    static let almostFull = Self.custom(AlmostFull.self)
}

private struct AlmostFull: CustomPresentationDetent {
    static func height(in context: Context) -> CGFloat? {
        // Prevent the background content from shrinking underneath the expanded sheet
        context.maxDetentValue - 0.5
    }
}
