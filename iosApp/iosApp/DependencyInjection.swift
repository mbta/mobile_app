//
//  DependencyInjection.swift
//  iosApp
//
//  Created by Brady, Kayla on 4/16/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct DIContainer: EnvironmentKey {
    let schedulesUseCase: ISchedulesUseCase

    init(schedulesUseCase: ISchedulesUseCase) {
        self.schedulesUseCase = schedulesUseCase
    }

    static var defaultValue: Self { defaultConfig }

    // TODO: - default to stub
    private static let defaultConfig = Self(schedulesUseCase: SchedulesUseCaseDI().schedulesUseCase)
}

extension EnvironmentValues {
    var injected: DIContainer {
        get { self[DIContainer.self] }
        set { self[DIContainer.self] = newValue }
    }
}
