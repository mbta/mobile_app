//
//  FcmInstallationIdContainer.swift
//  iosApp
//
//  Created by esimon on 11/28/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

class FcmInstallationIdContainer: ObservableObject {
    static let shared = FcmInstallationIdContainer()
    @Published var installationId: String?
}
