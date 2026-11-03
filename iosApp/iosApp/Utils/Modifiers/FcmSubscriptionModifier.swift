//
//  FcmSubscriptionModifier.swift
//  iosApp
//
//  Created by esimon on 11/28/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct FcmSubscriptionModifier: ViewModifier {
    let fcmInstallationId: String?
    let includeAccessibility: Bool

    @State var subscriptionsRepository: ISubscriptionsRepository = RepositoryDI().subscriptions

    @State var favorites: Favorites = LoadedFavorites.last
    @State var globalData: GlobalResponse?

    func updateSubscriptions(_ fcmInstallationId: String?) {
        if let fcmInstallationId {
            Task {
                let validFavorites = if let globalData {
                    FavoriteValidityKt.filterValidFavorites(favorites.routeStopDirection, global: globalData)
                } else {
                    favorites.routeStopDirection
                }
                let subscriptions = SubscriptionRequest.companion.fromFavorites(
                    favorites: validFavorites,
                    includeAccessibility: includeAccessibility
                )
                try await subscriptionsRepository.updateSubscriptions(
                    fcmInstallationId: fcmInstallationId,
                    subscriptions: subscriptions,
                    locale: NSLocalizedString("key/current_locale", comment: ""),
                )
            }
        }
    }

    func body(content: Content) -> some View {
        content
            .favorites($favorites)
            .global($globalData, errorKey: ErrorKey(sheets: [], id: "FcmSubscriptionModifier"))
            .onAppear { updateSubscriptions(fcmInstallationId) }
            .onChange(of: fcmInstallationId) { newInstallationId in updateSubscriptions(newInstallationId) }
            .onChange(of: globalData) { _ in updateSubscriptions(fcmInstallationId) }
            .enableInjection()
    }
}

public extension View {
    /** Update subscriptions on the backend when the FCM installation ID is set or changed. */
    func handleFcmInstallationIdSubscriptions(
        fcmInstallationId: String?,
        includeAccessibility: Bool,
    ) -> some View {
        modifier(FcmSubscriptionModifier(
            fcmInstallationId: fcmInstallationId,
            includeAccessibility: includeAccessibility,
        ))
    }
}
