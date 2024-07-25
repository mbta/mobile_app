//
//  ContentViewModel.swift
//  iosApp
//
//  Created by Brady, Kayla on 7/19/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
@_spi(Experimental) import MapboxMaps

class ContentViewModel: ObservableObject {
    @Published var configResponse: ApiResult<ConfigResponse>?
    @Published var searchEnabled: Bool
    @Published var dynamicMapKeyEnabled: Bool
    private var settings: Set<Setting> = []

    var configUseCase: ConfigUseCase
    var settingsRepo: ISettingsRepository

    init(configUseCase: ConfigUseCase = UsecaseDI().configUsecase,
         configResponse: ApiResult<ConfigResponse>? = nil,
         dynamicMapKeyEnabled: Bool = false,
         searchEnabled: Bool = false,
         settingsRepo: ISettingsRepository = RepositoryDI().settings) {
        self.configUseCase = configUseCase
        self.configResponse = configResponse
        self.dynamicMapKeyEnabled = dynamicMapKeyEnabled
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
            dynamicMapKeyEnabled = settings.first(where: { $0.key == .dynamicMapKey })?.isOn ?? false
        } catch {}
    }

    @MainActor func loadConfig() async {
        do {
            configResponse = try await configUseCase.getConfig()
        } catch {
            configResponse = ApiResultError(error: .init(code: nil, message: "\(error.localizedDescription)"))
        }
    }
}
