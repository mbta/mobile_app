//
//  StopExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 3/22/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Shared

extension Stop {
    var coordinate: CLLocationCoordinate2D {
        .init(latitude: latitude, longitude: longitude)
    }
}
