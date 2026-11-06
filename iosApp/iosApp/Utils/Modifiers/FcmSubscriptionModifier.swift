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
    let favoritesVM: IFavoritesViewModel

    func body(content: Content) -> some View {
        content
            .onAppear {
                favoritesVM.setLocale(locale: NSLocalizedString("key/current_locale", comment: ""))
                favoritesVM.setFcmInstallationId(fcmInstallationId: fcmInstallationId)
            }
            .onChange(of: fcmInstallationId) { newInstallationId in
                favoritesVM.setFcmInstallationId(fcmInstallationId: newInstallationId)
            }
            .enableInjection()
    }
}

public extension View {
    /** Update subscriptions on the backend when the FCM token is set or changed. */
    func handleFcmInstallationIdSubscriptions(
        fcmInstallationId: String?,
        favoritesVM: IFavoritesViewModel = ViewModelDI().favorites
    ) -> some View {
        modifier(FcmSubscriptionModifier(
            fcmInstallationId: fcmInstallationId,
            favoritesVM: favoritesVM
        ))
    }
}
