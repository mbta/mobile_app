//
//  RouteStopListView.swift
//  iosApp
//
//  Created by Melody Horn on 7/7/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

enum RouteDetailsRowContext: Equatable {
    case details(stop: Stop)
    case favorites(isFavorited: Bool, onTapStar: () -> Void)

    static func == (lhs: RouteDetailsRowContext, rhs: RouteDetailsRowContext) -> Bool {
        switch (lhs, rhs) {
        case let (.details(stop: lhs), .details(stop: rhs)):
            lhs == rhs
        case let (.favorites(isFavorited: lhs, onTapStar: _), .favorites(isFavorited: rhs, onTapStar: _)):
            lhs == rhs
        case (.details, .favorites):
            false
        case (.favorites, .details):
            false
        }
    }
}

struct RouteStopListView<RightSideContent: View>: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let context: RouteDetailsContext
    let globalData: GlobalResponse
    let onClick: (RouteDetailsRowContext) -> Void
    let onBack: () -> Void
    let onClose: () -> Void
    let errorBannerVM: ErrorBannerViewModel
    let defaultSelectedRouteId: String?
    let rightSideContent: (RouteDetailsRowContext) -> RightSideContent

    let favoritesUsecases: FavoritesUsecases
    @State var favorites: Set<RouteStopDirection>?
    let routeStopsRepository: IRouteStopsRepository
    @State var routeStops: RouteStopsResult?
    @State var stopList: RouteDetailsStopList?

    @State var selectedRouteId: String
    @State var selectedDirection: Int32

    let inspection = Inspection<Self>()

    init(
        lineOrRoute: RouteCardData.LineOrRoute,
        context: RouteDetailsContext,
        globalData: GlobalResponse,
        onClick: @escaping (RouteDetailsRowContext) -> Void,
        onBack: @escaping () -> Void,
        onClose: @escaping () -> Void,
        errorBannerVM: ErrorBannerViewModel,
        defaultSelectedRouteId: String? = nil,
        rightSideContent: @escaping (RouteDetailsRowContext) -> RightSideContent,
        routeStopsRepository: IRouteStopsRepository = RepositoryDI().routeStops,
        favoritesUsecases: FavoritesUsecases = UsecaseDI().favoritesUsecases,
    ) {
        self.lineOrRoute = lineOrRoute
        self.context = context
        self.globalData = globalData
        self.onClick = onClick
        self.onBack = onBack
        self.onClose = onClose
        self.errorBannerVM = errorBannerVM
        self.defaultSelectedRouteId = defaultSelectedRouteId
        self.rightSideContent = rightSideContent
        self.routeStopsRepository = routeStopsRepository
        self.favoritesUsecases = favoritesUsecases

        selectedRouteId = defaultSelectedRouteId ?? lineOrRoute.sortRoute.id
        let parameters = RouteDetailsStopList.RouteParameters(lineOrRoute: lineOrRoute, globalData: globalData)
        selectedDirection = Int32(truncating: parameters.availableDirections.first ?? 0)
    }

    private struct RouteStopsParams: Equatable {
        let routeId: String
        let directionId: Int32
    }

    private struct RouteStopListParams: Equatable {
        let routeId: String
        let directionId: Int32
        let routeStops: RouteStopsResult?
        let globalData: GlobalResponse
    }

    var body: some View {
        let routes = lineOrRoute.allRoutes.sorted(by: { $0.sortOrder <= $1.sortOrder })
        let parameters = RouteDetailsStopList.RouteParameters(lineOrRoute: lineOrRoute, globalData: globalData)

        VStack {
            SheetHeader(title: lineOrRoute.name, onBack: onBack, onClose: onClose)
            ErrorBanner(errorBannerVM)
            DirectionPicker(
                availableDirections: parameters.availableDirections.map { Int32(truncating: $0) },
                directions: parameters.directions,
                route: lineOrRoute.sortRoute,
                selectedDirectionId: selectedDirection,
                updateDirectionId: { selectedDirection = $0 }
            )
            .fixedSize(horizontal: false, vertical: true).padding(.horizontal, 14).padding(.vertical, 8)
            if case let .line(lineOrRoute) = onEnum(of: lineOrRoute), routes.count > 1 {
                lineRoutePicker(line: lineOrRoute.line, routes: routes)
            }
            routeStopList(stopList: stopList, onTapStop: onClick)
        }
        .onAppear {
            loadEverything()
            if !parameters.availableDirections.contains(where: { $0.intValue == selectedDirection }) {
                selectedDirection = Int32(truncating: parameters.availableDirections.first ?? 0)
            }
        }
        .onChange(of: RouteStopsParams(routeId: selectedRouteId, directionId: selectedDirection)) {
            loadRouteStops(routeId: $0.routeId, directionId: $0.directionId)
        }
        .onChange(of: RouteStopListParams(
            routeId: selectedRouteId,
            directionId: selectedDirection,
            routeStops: routeStops,
            globalData: globalData
        )) {
            loadStopList(
                routeId: $0.routeId,
                directionId: $0.directionId,
                routeStops: $0.routeStops,
                globalData: $0.globalData
            )
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder private func routeStopList(stopList: RouteDetailsStopList?,
                                            onTapStop: @escaping (RouteDetailsRowContext) -> Void) -> some View {
        if let stopList, stopList.directionId == Int32(selectedDirection) {
            ScrollView {
                VStack {
                    ForEach(Array(stopList.segments.enumerated()), id: \.offset) { segmentIndex, segment in
                        if segment.isTypical {
                            ForEach(Array(segment.stops.enumerated()), id: \.offset) { stopIndex, stop in
                                let stopPlacement = StopPlacement(
                                    isFirst: segmentIndex == stopList.segments.startIndex && stopIndex == segment.stops
                                        .startIndex,
                                    isLast: segmentIndex == stopList.segments
                                        .index(before: stopList.segments.endIndex) && stopIndex == segment.stops
                                        .index(before: segment.stops.endIndex),
                                    includeLineDiagram: segment.hasRouteLine
                                )
                                let stopRowContext = stopRowContext(stop.stop)
                                StopListRow(
                                    stop: stop.stop,
                                    onClick: { onTapStop(stopRowContext) },
                                    routeAccents: .init(route: lineOrRoute.sortRoute),
                                    stopListContext: .routeDetails,
                                    connectingRoutes: stop.connectingRoutes,
                                    stopPlacement: stopPlacement,
                                    descriptor: { EmptyView() },
                                    rightSideContent: { rightSideContent(stopRowContext) }
                                )
                            }
                        } else {
                            // TODO: CollapsableStopList
                        }
                    }
                }
            }
        } else {
            AnyView(loadingRouteStops())
        }
    }

    @ViewBuilder private func lineRoutePicker(line: Line, routes: [Route]) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(routes, id: \.id) { route in
                let selected = route.id == selectedRouteId
                let rowColor = selected ? Color.fill3 : Color.clear
                let textColor = selected ? Color.text : Color(hex: line.textColor)
                Button {
                    selectedRouteId = route.id
                } label: {
                    HStack(alignment: .center, spacing: 8) {
                        RoutePill(route: route, line: line, type: .fixed)
                        Text(((route.directionDestinations[Int(selectedDirection)] as? String?) ?? "") ?? "")
                            .foregroundStyle(textColor)
                            .font(Typography.title3Semibold)
                        Spacer()
                    }
                    .padding(8)
                    .background(rowColor)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .background(Color.deselectedToggle2.opacity(0.6))
        .background(Color(hex: line.color))
    }

    @ViewBuilder private func loadingRouteStops() -> some View {
        let loadingStops = LoadingPlaceholders.shared.routeDetailsStops(
            lineOrRoute: lineOrRoute,
            directionId: Int32(selectedDirection)
        )
        routeStopList(stopList: loadingStops, onTapStop: { _ in }).loadingPlaceholder()
    }

    private func loadEverything() {
        loadFavorites()
        loadRouteStops(routeId: selectedRouteId, directionId: selectedDirection)
        if let routeStops {
            loadStopList(
                routeId: selectedRouteId,
                directionId: selectedDirection,
                routeStops: routeStops,
                globalData: globalData
            )
        }
    }

    private func loadFavorites() {
        Task {
            favorites = try? await favoritesUsecases.getRouteStopDirectionFavorites()
        }
    }

    private func loadRouteStops(routeId: String, directionId: Int32) {
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "RouteStopListView.loadRouteStops",
                getData: { try await routeStopsRepository.getRouteStops(
                    routeId: routeId,
                    directionId: directionId
                ) },
                onSuccess: { routeStops = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    private func loadStopList(
        routeId: String,
        directionId: Int32,
        routeStops: RouteStopsResult?,
        globalData: GlobalResponse
    ) {
        Task {
            stopList = try? await RouteDetailsStopList.companion.fromPieces(
                routeId: routeId,
                directionId: directionId,
                routeStops: routeStops,
                globalData: globalData
            )
        }
    }

    private func isFavorite(_ routeStopDirection: RouteStopDirection) -> Bool {
        favorites?.contains(routeStopDirection) ?? false
    }

    private func stopRowContext(_ stop: Stop) -> RouteDetailsRowContext {
        switch onEnum(of: context) {
        case .details: .details(stop: stop)
        case .favorites: .favorites(
                isFavorited: isFavorite(.init(
                    route: selectedRouteId,
                    stop: stop.id,
                    direction: selectedDirection
                )),
                onTapStar: {}
            )
        }
    }
}
