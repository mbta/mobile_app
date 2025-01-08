//
//  CoordinateExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 3/21/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Foundation
import shared

extension CLLocationCoordinate2D {
    /// Rounds the double to decimal places value
    func isRoughlyEqualTo(_ other: CLLocationCoordinate2D?) -> Bool {
        guard let other else {
            return false
        }

        return latitude.rounded(toPlaces: 6) == other.latitude.rounded(toPlaces: 6)
            && longitude.rounded(toPlaces: 6) == other.longitude.rounded(toPlaces: 6)
    }

    /// Convenience conversion to Kotlin Position class
    var positionKt: GeojsonPosition {
        GeojsonPosition(longitude: longitude, latitude: latitude)
    }
}
