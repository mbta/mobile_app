//
//  FcmTokenContainer.swift
//  iosApp
//
//  Created by esimon on 11/28/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

class FcmTokenContainer: ObservableObject {
    static let shared = FcmTokenContainer()
    @Published var token: String?
}
