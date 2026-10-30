//
//  GlobalResponseExtension.swift
//  iosApp
//
//  Created by Melody Horn on 9/28/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared

extension GlobalResponse? {
    func isStopBlocklisted(_ stopId: String) -> Bool {
        self?.__isStopBlocklisted(stopId: stopId) ?? false
    }
}
