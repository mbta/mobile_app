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

    func updateSubscriptions(_ fcmInstallationId: String?, _ notificationsEnabled: Bool) {
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
            .onAppear { updateSubscriptions(fcmInstallationId, notificationsEnabled) }
            .onChange(of: fcmInstallationId) { newInstallationId in updateSubscriptions(
                newInstallationId,
                notificationsEnabled
            ) }
            .onChange(of: notificationsEnabled) { newNotifications in updateSubscriptions(
                fcmInstallationId,
                newNotifications
            ) }
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
