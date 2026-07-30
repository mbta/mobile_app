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
    let notificationsEnabled: Bool

    @State var subscriptionsRepository: ISubscriptionsRepository = RepositoryDI().subscriptions

    @State var favorites: Favorites = LoadedFavorites.last

    func updateSubscriptions(_ fcmInstallationId: String?, _ notificationsEnabled: Bool) {
        if let fcmInstallationId {
            Task {
                let subscriptions = SubscriptionRequest.companion.fromFavorites(
                    favorites: favorites.routeStopDirection,
                    includeAccessibility: includeAccessibility
                )
                try await subscriptionsRepository.updateSubscriptions(
                    fcmInstallationId: fcmInstallationId,
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
        notificationsEnabled: Bool,
    ) -> some View {
        modifier(FcmSubscriptionModifier(
            fcmInstallationId: fcmInstallationId,
            includeAccessibility: includeAccessibility,
            notificationsEnabled: notificationsEnabled,
        ))
    }
}
