//
//  ScheduleFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-12.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

class ScheduleFetcher: ObservableObject {
    @Published var schedules: ScheduleResponse?
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getSchedule(stopIds: [String]) async {
        do {
            let response = try await backend.getSchedule(stopIds: stopIds)
            schedules = response
        } catch {
            debugPrint(error)
        }
    }
}
