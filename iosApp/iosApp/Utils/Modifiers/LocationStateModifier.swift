//
//  LocationStateModifier.swift
//  iosApp
//
//  Created by esimon on 9/23/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import CoreLocation
import Foundation
import SwiftUI

struct LocationStateModifier: ViewModifier {
    @ObservedObject var locationDataManager: LocationDataManager
    let action: () -> Void

    func body(content: Content) -> some View {
        content
            // The location manager is created before any of these views exist, so the authorization
            // callback and the first location fix can both land before the `onChange` handlers below
            // have been registered. Running on appear as well means we don't sit waiting forever for
            // a change that already happened.
            .onAppear { action() }
            .onChange(of: locationDataManager.authorizationStatus) { _ in action() }
            .onChange(of: locationDataManager.currentLocation) { _ in action() }
            .enableInjection()
    }
}

public extension View {
    /**
     Run the specified action on appear and whenever the authorization status or current location
     changes, so that the action sees the latest location state no matter when it became available.
     */
    func withLocationStateHandler(
        _ locationDataManager: LocationDataManager,
        action: @escaping () -> Void
    ) -> some View {
        modifier(LocationStateModifier(locationDataManager: locationDataManager, action: action))
    }
}
