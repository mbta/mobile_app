//
//  PartialSheetDetent.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 3/27/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import UIKit

/// Replaces the use of PresentationDetent from the SwiftUI package as it's only available iOS 16+
public enum PartialSheetDetent: String, Comparable {
    @available(iOS 16, *)
    case small = "com.mbta.small"
    case medium = "com.apple.UIKit.medium"
    case large = "com.apple.UIKit.large"

    public static func < (lhs: Self, rhs: Self) -> Bool {
        lhs.ordinal < rhs.ordinal
    }

    public var uiKitDetent: UISheetPresentationController.Detent {
        switch self {
        case .small:
            if #available(iOS 16, *) {
                let smallDetentIdentifier = UISheetPresentationController.Detent.Identifier(Self.small.rawValue)
                return UISheetPresentationController.Detent.custom(identifier: smallDetentIdentifier) { _ in
                    200
                }
            } else {
                return .medium()
            }
        case .medium:
            return .medium()
        case .large:
            return .large()
        }
    }

    private var ordinal: Int {
        switch self {
        case .small:
            0
        case .medium:
            1
        case .large:
            2
        }
    }
}
