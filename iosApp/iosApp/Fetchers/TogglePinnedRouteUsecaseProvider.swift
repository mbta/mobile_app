//
//  TogglePinnedRouteUsecaseProvider.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 4/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class TogglePinnedRouteUsecaseProvider: ObservableObject {
    @Published var usecase: TogglePinnedRouteUsecase

    init(_ usecase: TogglePinnedRouteUsecase) {
        self.usecase = usecase
    }
}
