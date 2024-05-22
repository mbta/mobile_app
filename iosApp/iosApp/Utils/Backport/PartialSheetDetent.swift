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
    case medium = "com.mbta.medium"
    case large = "com.mbta.large"

    public static func < (lhs: Self, rhs: Self) -> Bool {
        lhs.ordinal < rhs.ordinal
    }

    public var uiKitDetent: UISheetPresentationController.Detent {
        switch self {
        case .small:
            customDetent(Self.small.rawValue) { _ in 195 }
        case .medium:
            customDetent(Self.medium.rawValue) { context in
                context.maximumDetentValue / 2
            }
        case .large:
            customDetent(Self.large.rawValue) { context in
                // Prevent the background content from shrinking underneath the expanded sheet
                context.maximumDetentValue - 1
            }
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

    private func customDetent(
        _ identifier: String,
        resolver: @escaping (_ context: any UISheetPresentationControllerDetentResolutionContext) -> CGFloat?
    ) -> UISheetPresentationController.Detent {
        let detentId = UISheetPresentationController.Detent.Identifier(identifier)
        return UISheetPresentationController.Detent.custom(identifier: detentId, resolver: resolver)
    }
}
