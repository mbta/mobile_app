//
//  FavoritesUsecasesExtension.swift
//  iosApp
//
//  Created by Melody Horn on 10/2/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared

extension FavoritesUsecases {
    func updateRouteStopDirections(
        newValues: [RouteStopDirection: FavoriteSettings?],
        context: EditFavoritesContext,
        defaultDirection: Int32,
        fcmToken: String?,
        includeAccessibility: Bool,
    ) async throws {
        try await __updateRouteStopDirections(
            newValues: newValues as [RouteStopDirection: Any],
            context: context,
            defaultDirection: defaultDirection,
            fcmToken: fcmToken,
            includeAccessibility: includeAccessibility,
            locale: NSLocalizedString("key/current_locale", comment: "")
        )
    }
}
