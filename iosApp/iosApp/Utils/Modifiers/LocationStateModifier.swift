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
    let action: (_ status: CLAuthorizationStatus?, _ location: CLLocation?) -> Void

    var authorizationStatus: CLAuthorizationStatus? { locationDataManager.authorizationStatus }
    var currentLocation: CLLocation? { locationDataManager.currentLocation }

    func body(content: Content) -> some View {
        content
            // The location manager is created before any of these views exist, so the authorization
            // callback and the first location fix can both land before the `onChange` handlers below
            // have been registered. Running on appear as well means we don't sit waiting forever for
            // a change that already happened.
            .onAppear { action(authorizationStatus, currentLocation) }
            .onChange(of: locationDataManager.authorizationStatus) { status in action(status, currentLocation) }
            .onChange(of: locationDataManager.currentLocation) { location in action(authorizationStatus, location) }
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
        action: @escaping (_ status: CLAuthorizationStatus?, _ location: CLLocation?) -> Void
    ) -> some View {
        modifier(LocationStateModifier(locationDataManager: locationDataManager, action: action))
    }
}
