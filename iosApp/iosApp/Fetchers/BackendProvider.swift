//
//  BackendProvider.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-03.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class BackendProvider: ObservableObject {
    @Published var backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }
}
