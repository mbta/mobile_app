//
//  ViewportExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 3/14/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@_spi(Experimental) import MapboxMaps

extension Viewport {
    var isFollowing: Bool {
        followPuck?.bearing == .constant(0)
    }

    var isOverview: Bool {
        overview != nil
    }
}
