//
//  EditFavoritesPage.swift
//  iosApp
//
//  Created by Kayla Brady on 7/10/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct EditFavoritesPage: View {
    let viewModel: IFavoritesViewModel
    let onClose: () -> Void

    @State var globalResponse: GlobalResponse?

    let errorBannerVM: ErrorBannerViewModel
    let globalRepository: IGlobalRepository = RepositoryDI().global

    var body: some View {
        VStack {
            SheetHeader(
                title: NSLocalizedString("Edit Favorites", comment: "Title for flow to edit favorites"),
                onClose: onClose,
                closeText: NSLocalizedString("Done", comment: "Button text for closing flow")
            )
            Text(verbatim: "TODO")
            Spacer()
        }
        .background(Color.fill2)
        .onAppear {
            loadGlobal()
        }
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            globalResponse = globalData
        }
    }

    private func loadGlobal() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "EditDetailsPage.loadGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadGlobal
            )
        }
    }
}
