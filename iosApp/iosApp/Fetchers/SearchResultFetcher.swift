//
//  SearchResultFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class SearchResultFetcher: ObservableObject {
    @Published var results: SearchResults?
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getSearchResults(query: String) async throws {
        if (query.isEmpty) {
            results = nil
            return
        }
        let response = try await backend.getSearchResults(query: query)
        results = response.data
    }
}
