//
//  PinnedRouteRepositoryProvider.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 4/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class PinnedRouteRepositoryProvider: ObservableObject {
    @Published var repository: any PinnedRoutesRepository

    init(_ repository: any PinnedRoutesRepository) {
        self.repository = repository
    }
}
