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

    @State var favoritesVM: IFavoritesViewModel = ViewModelDI().favorites

    func body(content: Content) -> some View {
        content
            .onAppear {
                favoritesVM.setLocale(locale: NSLocalizedString("key/current_locale", comment: ""))
                favoritesVM.setFcmToken(fcmToken: fcmToken)
            }
            .onChange(of: fcmToken) { newToken in favoritesVM.setFcmToken(fcmToken: newToken) }
            .enableInjection()
    }
}

public extension View {
    /** Update subscriptions on the backend when the FCM token is set or changed. */
    func handleFcmTokenSubscriptions(
        fcmToken: String?,
    ) -> some View {
        modifier(FcmSubscriptionModifier(
            fcmToken: fcmToken,
        ))
    }
}
