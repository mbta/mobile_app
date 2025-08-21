//
//  RouteDetailsView.swift
//  iosApp
//
//  Created by Melody Horn on 7/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteDetailsView: View {
    let selectionId: String
    let context: RouteDetailsContext
    let onOpenStopDetails: (String) -> Void
    let onBack: () -> Void
    let onClose: () -> Void
    let errorBannerVM: IErrorBannerViewModel

    @State var globalData: GlobalResponse?
    @State private var lineOrRoute: RouteCardData.LineOrRoute?
    let globalRepository: IGlobalRepository = RepositoryDI().global

    var body: some View {
        ScrollView([]) {
            if let globalData, let lineOrRoute {
                RouteStopListView(
                    lineOrRoute: lineOrRoute,
                    context: context,
                    globalData: globalData,
                    onClick: { row in
                        switch row {
                        case let .details(stop: stop): onOpenStopDetails(stop.id)
                        case let .favorites(isFavorited: _, onTapStar: onTapStar): onTapStar()
                        }
                    },
                    onBack: onBack,
                    onClose: onClose,
                    errorBannerVM: errorBannerVM,
                    defaultSelectedRouteId: selectionId == lineOrRoute.id ? nil : selectionId,
                    rightSideContent: rightSideContent
                )
            } else {
                loadingBody()
            }
        }
        .onAppear {
            getGlobal()
        }
        .onChange(of: globalData) { globalData in
            lineOrRoute = globalData?.getLineOrRoute(lineOrRouteId: selectionId)
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
                errorKey: "NearbyTransitView.getGlobal",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: { @MainActor in getGlobal() }
            )
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let objects = ObjectCollectionBuilder()
        let mockRoute = RouteCardData.LineOrRouteRoute(route: objects.route { _ in })
        let mockGlobal = GlobalResponse(objects: objects)
        let toastVM = MockToastViewModel(initialState: ToastViewModel.StateHidden())
        RouteStopListContentView(
            lineOrRoute: mockRoute,
            parameters: .init(lineOrRoute: mockRoute, globalData: mockGlobal),
            selectedDirection: 0,
            setSelectedDirection: { _ in },
            selectedRouteId: mockRoute.id,
            setSelectedRouteId: { _ in },
            stopList: .none,
            context: context,
            globalData: globalData ?? GlobalResponse(objects: .init()),
            onClick: { _ in },
            onBack: {},
            onClose: {},
            errorBannerVM: errorBannerVM,
            rightSideContent: rightSideContent,
            toastVM: toastVM
        )
        .loadingPlaceholder()
    }

    @ViewBuilder private func rightSideContent(rowContext: RouteDetailsRowContext) -> some View {
        switch rowContext {
        case .details:
            Image(.faChevronRight)
                .resizable().frame(width: 8, height: 14).padding(.trailing, 8)
                .foregroundStyle(Color.deemphasized)
        case let .favorites(isFavorited: isFavorited, onTapStar: _):
            var starColor: Color {
                if let lineOrRoute {
                    Color(hex: lineOrRoute.backgroundColor)
                } else {
                    Color.text
                }
            }
            StarIcon(starred: isFavorited, color: starColor)
                .padding(.trailing, 8)
                .accessibilityLabel(isFavorited ? Text("favorite stop") : Text(verbatim: ""))
        }
    }
}
