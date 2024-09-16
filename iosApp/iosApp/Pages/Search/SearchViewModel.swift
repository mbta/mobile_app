//
//  SearchViewModel.swift
//  iosApp
//
//  Created by esimon on 9/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
@_spi(Experimental) import MapboxMaps

class SearchViewModel: ObservableObject {
    @Published var routeResultsEnabled: Bool
    private var settings: Set<Setting> = []

    var settingsRepo: ISettingsRepository

    init(
        routeResultsEnabled: Bool = false,
        settingsRepo: ISettingsRepository = RepositoryDI().settings
    ) {
        self.routeResultsEnabled = routeResultsEnabled
        self.settingsRepo = settingsRepo
    }

    @MainActor func loadSettings() async {
        do {
            let settings = try await settingsRepo.getSettings()
            routeResultsEnabled = settings.first(where: { $0.key == .searchRouteResults })?.isOn ?? false
        } catch {}
    }
}
