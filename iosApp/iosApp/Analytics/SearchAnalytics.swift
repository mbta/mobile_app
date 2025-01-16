//
//  SearchAnalytics.swift
//  iosApp
//
//  Created by Horn, Melody on 2025-01-13.
//  Copyright © 2025 MBTA. All rights reserved.
//

import FirebaseAnalytics

protocol SearchAnalytics {
    func performedSearch(query: String)
}

extension AnalyticsProvider: SearchAnalytics {
    func performedSearch(query: String) {
        logEvent("search", parameters: ["query": query])
    }
}
