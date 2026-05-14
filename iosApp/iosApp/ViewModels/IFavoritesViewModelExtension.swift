//
//  IFavoritesViewModelExtension.swift
//  iosApp
//
//  Created by Melody Horn on 9/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared

extension IFavoritesViewModel {
    func updateFavorites(
        updatedFavorites: [RouteStopDirection: FavoriteSettings?],
        context: EditFavoritesContext,
        defaultDirection: Int32,
        fcmToken: String?,
    ) {
        __updateFavorites(
            updatedFavorites: updatedFavorites,
            context: context,
            defaultDirection: .init(int: defaultDirection),
            fcmToken: fcmToken,
            locale: NSLocalizedString("key/current_locale", comment: ""),
        )
    }
}
