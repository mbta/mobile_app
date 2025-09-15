//
//  GlobalModifier.swift
//  iosApp
//
//  Created by esimon on 9/3/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct GlobalModifier: ViewModifier {
    var globalRepository: IGlobalRepository = RepositoryDI().global
    @Binding var global: GlobalResponse?
    let errorKey: String

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            global = globalData
        }
    }

    private func loadGlobal() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorKey: "\(errorKey).loadGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadGlobal
            )
        }
    }

    func body(content: Content) -> some View {
        content.task { loadGlobal() }
    }
}

public extension View {
    func global(_ global: Binding<GlobalResponse?>, errorKey: String) -> some View {
        modifier(GlobalModifier(global: global, errorKey: errorKey))
    }
}
