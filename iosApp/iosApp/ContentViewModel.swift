//
//  ContentViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 7/19/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
@_spi(Experimental) import MapboxMaps
import shared

class ContentViewModel: ObservableObject {
    @Published var configResponse: ApiResult<ConfigResponse>?
    @Published var searchEnabled: Bool
    private var settings: Set<Setting> = []

    var configUseCase: ConfigUseCase
    var settingsRepo: ISettingsRepository

    init(configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
         configResponse: ApiResult<ConfigResponse>? = nil,
         searchEnabled: Bool = false,
         settingsRepo: ISettingsRepository = RepositoryDI().settings) {
        self.configUseCase = configUseCase
        self.configResponse = configResponse
        self.searchEnabled = searchEnabled
        self.settingsRepo = settingsRepo
    }

    func configureMapboxToken(token: String) {
        MapboxOptions.accessToken = token
    }

    @MainActor func loadSettings() async {
        do {
            let settings = try await settingsRepo.getSettings()
            searchEnabled = settings.first(where: { $0.key == .search })?.isOn ?? false
        } catch {}
    }

    @MainActor func loadConfig() async {
        do {
            configResponse = try await configUseCase.getConfig()
        } catch {
            configResponse = ApiResultError(code: nil, message: "\(error.localizedDescription)")
        }
    }
}
