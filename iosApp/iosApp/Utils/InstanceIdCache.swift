//
//  InstanceIdCache.swift
//  iosApp
//
//  Created by esimon on 3/6/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import FirebaseInstallations
import Shared
import SwiftUI

protocol IInstanceIdCache {
    var instanceId: String? { get }
}

/// Stores the state of the `Settings` so that they can be read instantly from anywhere once they have been loaded.
class InstanceIdCache: ObservableObject, IInstanceIdCache {
    static let shared = InstanceIdCache()

    @Published var instanceId: String?

    init() {
        Task {
            do {
                instanceId = try await Installations.installations().installationID()
            } catch {
                print("InstanceIdCache - Error fetching instance ID: \(error)")
            }
        }
    }
}

class MockInstanceIdCache: ObservableObject, IInstanceIdCache {
    @Published var instanceId: String?

    init(instanceId: String? = nil) {
        self.instanceId = instanceId
    }
}
