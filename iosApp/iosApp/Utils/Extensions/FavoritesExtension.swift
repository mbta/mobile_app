//
//  FavoritesExtension.swift
//  iosApp
//
//  Created by esimon on 9/4/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension Favorites {
    // Extension just to make it so that the param label is not required
    func isFavorite(_ rsd: RouteStopDirection) -> Bool {
        isFavorite(rsd: rsd)
    }
}
