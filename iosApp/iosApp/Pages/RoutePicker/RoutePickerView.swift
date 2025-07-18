//
//  RoutePickerView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/14/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct RoutePickerView: View {
    let context: RouteDetailsContext
    let path: RoutePickerPath
    let errorBannerVM: ErrorBannerViewModel
    let onOpenRouteDetails: (String, RouteDetailsContext) -> Void
    let onClose: () -> Void

    @State var globalData: GlobalResponse?
    @State var routes: [RouteCardData.LineOrRoute] = []
    let globalRepository: IGlobalRepository = RepositoryDI().global
    var errorBannerRepository = RepositoryDI().errorBanner

    let inspection = Inspection<Self>()

    var body: some View {
        ZStack {
            Color.fill2.edgesIgnoringSafeArea(.all)
            VStack {
                SheetHeader(
                    title: NSLocalizedString("Add favorite stops", comment: "Header for add favorites flow"),
                    rightActionContents: {
                        NavTextButton(
                            string: NSLocalizedString("Done", comment: "Button text for closing flow"),
                            backgroundColor: Color.text.opacity(0.6),
                            textColor: Color.fill3,
                            action: onClose
                        )
                    }
                )
                ErrorBanner(errorBannerVM)
                ScrollView {
                    VStack(alignment: .leading) {
                        Text("Subway")
                            .font(Typography.subheadlineSemibold)
                            .padding(.leading, 16)
                            .padding(.top, 22)
                            .padding(.bottom, 2)
                        ForEach(routes, id: \.self) { route in
                            RoutePickerRootRow(
                                route: route,
                                onTap: {
                                    onOpenRouteDetails(route.id, context)
                                }
                            )
                        }
                    }
                    .padding(.horizontal, 14)
                    .padding(.top, 10)
                }
            }
        }
        .onAppear { getGlobal() }
        .onChange(of: globalData) { globalData in
            routes = globalData?.getRoutesForPicker(path: path) ?? []
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            self.globalData = globalData
        }
    }

    func getGlobal() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "RoutePickerView.getGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: { @MainActor in getGlobal() }
            )
        }
    }
}
