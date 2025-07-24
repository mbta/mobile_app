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
    let onOpenPickerPath: (RoutePickerPath, RouteDetailsContext) -> Void
    let onClose: () -> Void
    let onBack: () -> Void

    @State var globalData: GlobalResponse?
    @State var routes: [RouteCardData.LineOrRoute] = []
    let globalRepository: IGlobalRepository = RepositoryDI().global
    var errorBannerRepository = RepositoryDI().errorBanner

    let inspection = Inspection<Self>()

    private let modes = [
        RoutePickerPath.Bus.shared,
        RoutePickerPath.Silver.shared,
        RoutePickerPath.CommuterRail.shared,
        RoutePickerPath.Ferry.shared,
    ]

    private var headerTitle: String {
        switch onEnum(of: path) {
        case .root:
            switch onEnum(of: context) {
            case .favorites:
                NSLocalizedString("Add favorite stops", comment: "Header for add favorites flow")
            case .details: "" // TODO: Implement details header
            }
        case .bus: NSLocalizedString("Bus", comment: "bus")
        case .silver: "Silver Line"
        case .commuterRail: "Commuter Rail"
        case .ferry: NSLocalizedString("Ferry", comment: "ferry")
        }
    }

    var body: some View {
        ZStack {
            path.backgroundColor.edgesIgnoringSafeArea(.all)
            VStack {
                header
                ErrorBanner(errorBannerVM)
                ScrollView {
                    Group {
                        if path is RoutePickerPath.Root {
                            rootContent
                        } else {
                            let displayedRoutes = routes // TODO: Search result state
                            VStack(spacing: 0) {
                                if !displayedRoutes.isEmpty {
                                    ForEach(displayedRoutes, id: \.self) { route in
                                        RoutePickerRow(route: route, onTap: { onOpenRouteDetails(route.id, context) })
                                        if route != displayedRoutes.last { HaloSeparator() }
                                    }
                                }
                            }
                            .background(Color.fill3)
                            .withRoundedBorder(color: path.haloColor, width: 2)
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
        .onChange(of: path) { newPath in
            withAnimation {
                routes = globalData?.getRoutesForPicker(path: newPath) ?? []
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    private var header: some View {
        SheetHeader(
            title: headerTitle,
            titleColor: path.textColor,
            onBack: !(path is RoutePickerPath.Root) ? onBack : nil,
            rightActionContents: {
                NavTextButton(
                    string: NSLocalizedString("Done", comment: "Button text for closing flow"),
                    backgroundColor: Color.text.opacity(0.6),
                    textColor: Color.fill3,
                    action: onClose
                )
            }
        )
    }

    private var rootContent: some View {
        VStack(alignment: .leading) {
            ForEach(modes, id: \.self) { mode in
                RoutePickerRootRow(path: mode, onTap: { onOpenPickerPath(mode, context) })
            }
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
