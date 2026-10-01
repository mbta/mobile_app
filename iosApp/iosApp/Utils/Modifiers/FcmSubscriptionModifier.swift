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
    let fcmToken: String?
    let includeAccessibility: Bool
    let notificationsEnabled: Bool

    @State var subscriptionsRepository: ISubscriptionsRepository = RepositoryDI().subscriptions

    @State var favorites: Favorites = LoadedFavorites.last
    @State var globalData: GlobalResponse?

    func updateSubscriptions(_ fcmToken: String?, _ notificationsEnabled: Bool) {
        if let fcmToken {
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
                    fcmToken: fcmToken,
                    subscriptions: subscriptions,
                    locale: NSLocalizedString("key/current_locale", comment: ""),
                    notificationsEnabled: notificationsEnabled,
                )
            }
        }
    }

    func body(content: Content) -> some View {
        content
            .favorites($favorites)
            .global($globalData, errorKey: ErrorKey(sheets: [], id: "FcmSubscriptionModifier"))
            .onAppear { updateSubscriptions(fcmToken, notificationsEnabled) }
            .onChange(of: fcmToken) { newToken in updateSubscriptions(newToken, notificationsEnabled) }
            .onChange(of: notificationsEnabled) { newNotifications in updateSubscriptions(fcmToken, newNotifications) }
            .onChange(of: globalData) { _ in updateSubscriptions(fcmToken, notificationsEnabled) }
            .enableInjection()
    }
}

public extension View {
    /** Update subscriptions on the backend when the FCM token is set or changed. */
    func handleFcmTokenSubscriptions(
        fcmToken: String?,
        includeAccessibility: Bool,
        notificationsEnabled: Bool,
    ) -> some View {
        modifier(FcmSubscriptionModifier(
            fcmToken: fcmToken,
            includeAccessibility: includeAccessibility,
            notificationsEnabled: notificationsEnabled,
        ))
    }
}
