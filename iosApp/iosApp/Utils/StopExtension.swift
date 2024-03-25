//
//  StopExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 3/22/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import shared

extension Stop {
    var coordinate: CLLocationCoordinate2D {
        .init(latitude: latitude, longitude: longitude)
    }

    // TODO: This should be moved into a stop caching layer once it exists
    func resolveParent(_ stopMap: [String: Stop]) -> Stop? {
        guard let parentStationId else { return self }
        guard let parent = stopMap[parentStationId] else { return nil }
        return parent.resolveParent(stopMap)
    }
}
