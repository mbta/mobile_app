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
    var searchRoutesViewModel: ISearchRoutesViewModel = ViewModelDI().searchRoutes
    let onOpenRouteDetails: (String, RouteDetailsContext) -> Void
    let onOpenPickerPath: (RoutePickerPath, RouteDetailsContext) -> Void
    let onClose: () -> Void
    let onBack: () -> Void

    @State var globalData: GlobalResponse?
    @State var routes: [RouteCardData.LineOrRoute] = []
    @State var searchVMState: SearchRoutesViewModel.State = SearchRoutesViewModel.StateUnfiltered()
    @StateObject var searchObserver = TextFieldObserver()
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
                NSLocalizedString(
                    "Add favorite stops",
                    comment: "Header for the route details picker when entering from the favorites page to select favorite stops"
                )
            case .details: "" // TODO: Implement details header
            }
        case .bus: NSLocalizedString("Bus", comment: "Label for bus routes in the route picker view")
        case .silver: "Silver Line"
        case .commuterRail: "Commuter Rail"
        case .ferry: NSLocalizedString("Ferry", comment: "Label for ferry routes in the route picker view")
        }
    }

    private var isRootPath: Bool { path is RoutePickerPath.Root }

    var body: some View {
        ZStack {
            path.backgroundColor.edgesIgnoringSafeArea(.all)
            VStack(spacing: 0) {
                header
                ErrorBanner(errorBannerVM)
                if !isRootPath {
                    SearchInput(
                        searchObserver: searchObserver,
                        hint: NSLocalizedString(
                            "Filter routes",
                            comment: "Hint text for the search input in the route picker view"
                        ),
                        onClear: { searchObserver.isFocused = false }
                    )
                    .padding(.top, 16)
                    .padding(.bottom, 8)
                    .padding(.horizontal, 16)
                }
                ScrollView {
                    Group {
                        if isRootPath {
                            rootContent
                                .padding(.top, 16)
                        } else {
                            let displayedRoutes = switch onEnum(of: searchVMState) {
                            case .unfiltered, .error: routes
                            case let .results(state):
                                state.routeIds.compactMap { routeId in
                                    routes.first(where: { route in route.id == routeId })
                                }
                            }
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
                            .padding(.top, 16)
                            footer(emptyResults: displayedRoutes.isEmpty)
                                .padding(.top, 16)
                        }
                    }
                    .padding(.horizontal, 14)
                }
            }
        }
        .onAppear {
            getGlobal()
            searchRoutesViewModel.setPath(path: path)
        }
        .onChange(of: globalData) { globalData in
            routes = globalData?.getRoutesForPicker(path: path) ?? []
        }
        .onChange(of: path) { newPath in
            withAnimation {
                routes = globalData?.getRoutesForPicker(path: newPath) ?? []
            }
            searchRoutesViewModel.setPath(path: newPath)
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .task {
            for await model in searchRoutesViewModel.models {
                searchVMState = model
            }
        }
        .onChange(of: searchObserver.searchText) { query in
            searchRoutesViewModel.setQuery(query: query)
        }
    }

    private var header: some View {
        SheetHeader(
            title: headerTitle,
            titleColor: path.textColor,
            buttonColor: Color.text.opacity(0.6),
            buttonTextColor: Color.fill3,
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
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h2)
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

    @ViewBuilder
    private func footer(emptyResults: Bool) -> some View {
        if searchVMState is SearchRoutesViewModel.StateResults {
            VStack(spacing: 2) {
                if emptyResults {
                    Text(
                        String(
                            format: NSLocalizedString(
                                "No matching %1$@ routes",
                                comment: "Text to indicate there's no matching results in route picker view"
                            ),
                            path.routeType.typeText(isOnly: true)
                        )
                    )
                    .foregroundColor(path.textColor)
                    .font(Typography.bodySemibold)
                }
                Text("To find stops, select a route first")
                    .foregroundColor(path.textColor)
                    .font(Typography.body)
            }
            .frame(maxWidth: .infinity)
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
