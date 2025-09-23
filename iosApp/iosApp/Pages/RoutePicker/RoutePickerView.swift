//
//  RoutePickerView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/14/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Combine
import Foundation
import Shared
import SwiftUI

struct RoutePickerView: View {
    let context: RouteDetailsContext
    let path: RoutePickerPath
    let errorBannerVM: IErrorBannerViewModel
    var searchRoutesViewModel: ISearchRoutesViewModel = ViewModelDI().searchRoutes
    let onOpenRouteDetails: (String, RouteDetailsContext) -> Void
    let onOpenPickerPath: (RoutePickerPath, RouteDetailsContext) -> Void
    let onClose: () -> Void
    let onBack: () -> Void

    @State var globalData: GlobalResponse?
    @State var routes: [LineOrRoute] = []
    @State var routeSearchResults: [LineOrRoute] = []

    @State var searchVMState: SearchRoutesViewModel.State = SearchRoutesViewModel.StateUnfiltered()
    @StateObject var searchObserver = TextFieldObserver()

    let scrollSubject = PassthroughSubject<String, Never>()

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

    func routeSearchResultsForVMState(state: SearchRoutesViewModel.State,
                                      routes: [LineOrRoute]) -> [LineOrRoute] {
        switch onEnum(of: state) {
        case .unfiltered, .error: routes
        case let .results(state):
            state.routeIds.compactMap { routeId in
                routes.first(where: { route in route.id == routeId })
            }
        }
    }

    var body: some View {
        ZStack {
            path.backgroundColor.edgesIgnoringSafeArea(.all)
            VStack(spacing: 0) {
                header
                ErrorBanner(errorBannerVM, padding: .init([.horizontal, .top], 16))
                if isRootPath {
                    ScrollView {
                        rootContent
                            .padding(.top, 16)
                            .padding(.horizontal, 14)
                    }
                } else {
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
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(spacing: 0) {
                                if !routeSearchResults.isEmpty {
                                    ForEach(routeSearchResults, id: \.id) { route in
                                        RoutePickerRow(route: route, onTap: { onOpenRouteDetails(route.id, context) })
                                        if route != routeSearchResults.last { HaloSeparator() }
                                    }
                                }
                            }
                            .background(Color.fill3)
                            .withRoundedBorder(color: path.haloColor, width: 2)
                            .padding(.top, 16)
                            footer(emptyResults: routeSearchResults.isEmpty)
                                .padding(.top, 16)
                        }

                        .padding(.horizontal, 14)
                        .onReceive(scrollSubject) { id in
                            withAnimation {
                                proxy.scrollTo(id, anchor: .top)
                            }
                        }
                    }.onChange(of: routeSearchResults) { newRouteSearchResults in
                        if let firstRoute = newRouteSearchResults.first, !isRootPath {
                            scrollSubject.send(firstRoute.id)
                        }
                    }
                }
            }.ignoresSafeArea(.keyboard, edges: .bottom)
        }
        .global($globalData, errorKey: "RoutePickerView")
        .onAppear {
            searchRoutesViewModel.setPath(path: path)
        }
        .onChange(of: globalData) { globalData in
            routes = globalData?.getRoutesForPicker(path: path) ?? []
        }
        .onChange(of: routes) { newRoutes in
            routeSearchResults = routeSearchResultsForVMState(state: searchVMState, routes: newRoutes)
        }
        .onChange(of: searchVMState) { newSearchVMState in
            routeSearchResults = routeSearchResultsForVMState(state: newSearchVMState, routes: routes)
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
            buttonColor: Color.routeColorContrast,
            buttonTextColor: Color.routeColorContrastText,
            onBack: !(path is RoutePickerPath.Root) ? onBack : nil,
            rightActionContents: {
                NavTextButton(
                    string: NSLocalizedString("Done", comment: "Button text for closing flow"),
                    backgroundColor: Color.routeColorContrast,
                    textColor: Color.routeColorContrastText,
                    height: 32,
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
}
