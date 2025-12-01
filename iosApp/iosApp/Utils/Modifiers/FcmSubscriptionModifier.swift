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

    func updateSubscriptions(_ fcmToken: String?, _ notificationsEnabled: Bool) {
        if notificationsEnabled, let fcmToken {
            Task {
                let subscriptions = SubscriptionRequest.companion.fromFavorites(
                    favorites: favorites.routeStopDirection,
                    includeAccessibility: includeAccessibility
                )
                try await subscriptionsRepository.updateSubscriptions(fcmToken: fcmToken, subscriptions: subscriptions)
            }
        }
    }

    func body(content: Content) -> some View {
        content
            .favorites($favorites)
            .onAppear { updateSubscriptions(fcmToken, notificationsEnabled) }
            .onChange(of: fcmToken) { newToken in updateSubscriptions(newToken, notificationsEnabled) }
            .onChange(of: notificationsEnabled) { newNotifications in updateSubscriptions(fcmToken, newNotifications) }
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
