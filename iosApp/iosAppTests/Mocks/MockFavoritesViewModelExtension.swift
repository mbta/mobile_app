//
//  MockFavoritesViewModelExtension.swift
//  iosApp
//
//  Created by Melody Horn on 9/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension MockFavoritesViewModel {
    var onUpdateFavorites: ([RouteStopDirection: FavoriteSettings?]) -> Void {
        get {
            { _ in }
        }
        set {
            __onUpdateFavorites = { rawDict in
                let typedDict = rawDict.mapValues { $0 as? FavoriteSettings }
                newValue(typedDict)
            }
        }
    }
}
