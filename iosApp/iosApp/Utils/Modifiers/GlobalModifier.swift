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
    let errorKey: ErrorKey

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            global = globalData
        }
    }

    private func fetchGlobal() async {
        await fetchApi(
            errorKey: errorKey.withSuffix(suffix: "loadGlobal"),
            getData: { try await globalRepository.getGlobalData() },
            onRefreshAfterError: { Task { await fetchGlobal() } }
        )
    }

    func body(content: Content) -> some View {
        content
            .task(priority: .high) { await activateGlobalListener() }
            .task { await fetchGlobal() }
            .enableInjection()
    }
}

public extension View {
    func global(_ global: Binding<GlobalResponse?>, errorKey: ErrorKey) -> some View {
        modifier(GlobalModifier(global: global, errorKey: errorKey))
    }
}
