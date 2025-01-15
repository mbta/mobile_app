//
//  CurrentAppVersionRepository.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-14.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import shared

class CurrentAppVersionRepository: ICurrentAppVersionRepository {
    func getCurrentAppVersion() -> AppVersion? {
        guard let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String else { return nil }
        return AppVersion.companion.parse(version: version)
    }
}
