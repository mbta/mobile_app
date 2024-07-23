//
//  AppCheckProviderFactory.swift
//  iosApp
//
//  Created by Brady, Kayla on 7/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import FirebaseAppCheck
import FirebaseCore
import Foundation

class CustomAppCheckProviderFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        AppAttestProvider(app: app)
    }
}
