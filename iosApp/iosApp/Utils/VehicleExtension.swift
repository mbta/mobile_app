//
//  VehicleExtension.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import Shared

extension Vehicle {
    var coordinate: CLLocationCoordinate2D {
        .init(latitude: latitude, longitude: longitude)
    }
}
