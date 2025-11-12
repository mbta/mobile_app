//
//  IFavoritesViewModelExtension.swift
//  iosApp
//
//  Created by Melody Horn on 9/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension IFavoritesViewModel {
    func updateFavorites(
        updatedFavorites: [RouteStopDirection: FavoriteSettings?],
        context: EditFavoritesContext,
        defaultDirection: Int32,
    ) {
        print("~~~ update in VM extension")
        __updateFavorites(
            updatedFavorites: updatedFavorites,
            context: context,
            defaultDirection: defaultDirection,
            fcmToken: nil, // TODO:
            includeAccessibility: false // TODO:
        )
    }
}
