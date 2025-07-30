//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@_spi(Experimental) import MapboxMaps
import os
import Shared
import SwiftUI

struct HomeMapView: View {
    var analytics: Analytics = AnalyticsProvider.shared
    @ObservedObject var contentVM: ContentViewModel
    @ObservedObject var mapVM: iosApp.MapViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @Environment(\.colorScheme) var colorScheme

    var errorBannerRepository: IErrorBannerStateRepository

    var globalRepository: IGlobalRepository

    var railRouteShapeRepository: IRailRouteShapeRepository
    @State var railRouteShapes: MapFriendlyRouteResponse?

    var stopRepository: IStopRepository
    @State var globalMapData: GlobalMapData?
    @State var stopMapData: StopMapResponse?

    @State var vehiclesRepository: IVehiclesRepository
    @State var vehiclesData: [Vehicle]?

    @StateObject var locationDataManager: LocationDataManager
    @Binding var sheetHeight: CGFloat

    @State private var recenterButton: ViewAnnotation?
    @State private var now = Date.now
    @State var lastNavEntry: SheetNavigationStackEntry?

    let inspection = Inspection<Self>()
    let log = Logger()

    private var shouldShowLoadedLocation: Bool {
        !viewportProvider.viewport.isFollowing
            && nearbyVM.navigationStack.lastSafe().allowTargeting
            && !nearbyVM.isTargeting
    }

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        contentVM: ContentViewModel,
        mapVM: iosApp.MapViewModel,
        nearbyVM: NearbyViewModel,
        viewportProvider: ViewportProvider,
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        railRouteShapeRepository: IRailRouteShapeRepository = RepositoryDI().railRouteShapes,
        stopRepository: IStopRepository = RepositoryDI().stop,
        vehiclesData: [Vehicle]? = nil,
        vehiclesRepository: IVehiclesRepository = RepositoryDI().vehicles,
        locationDataManager: LocationDataManager,
        sheetHeight: Binding<CGFloat>,
        globalMapData: GlobalMapData? = nil
    ) {
        self.globalRepository = globalRepository
        self.contentVM = contentVM
        self.mapVM = mapVM
        self.nearbyVM = nearbyVM
        self.viewportProvider = viewportProvider
        self.errorBannerRepository = errorBannerRepository
        self.railRouteShapeRepository = railRouteShapeRepository
        self.stopRepository = stopRepository
        self.vehiclesData = vehiclesData
        self.vehiclesRepository = vehiclesRepository
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
        _sheetHeight = sheetHeight
        _globalMapData = State(wrappedValue: globalMapData)
    }

    var body: some View {
        realtimeResponsiveMap
            .overlay(alignment: .center) {
                if nearbyVM.isTargeting {
                    crosshairs
                }
            }
            .task { loadGlobalData() }
            .task { loadRouteShapes() }
            .onChange(of: lastNavEntry) { [oldNavEntry = lastNavEntry] nextNavEntry in
                handleLastNavChange(oldNavEntry: oldNavEntry, nextNavEntry: nextNavEntry)
            }
            .onChange(of: mapVM.routeSourceData) { _ in
                updateRouteSource()
                if let layerManager = mapVM.layerManager {
                    addLayers(layerManager)
                }
            }
            .onChange(of: mapVM.stopLayerState) { _ in
                if let layerManager = mapVM.layerManager {
                    addLayers(layerManager)
                }
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .onChange(of: viewportProvider.isManuallyCentering) { isManuallyCentering in
                guard isManuallyCentering, nearbyVM.navigationStack.lastSafe().allowTargeting else { return }
                // This will be set to false after nearby is loaded to avoid the crosshair dissapearing and re-appearing
                nearbyVM.isTargeting = true
            }
            .onChange(of: contentVM.onboardingScreensPending) { _ in
                checkOnboardingLoaded()
            }
            .onAppear {
                checkOnboardingLoaded()
            }
            .onDisappear {
                mapVM.layerManager = nil
            }
    }

    @ViewBuilder
    var realtimeResponsiveMap: some View {
        staticResponsiveMap
            .onChange(of: nearbyVM.alerts) { _ in
                handleGlobalMapDataChange(now: now)
            }
            .onChange(of: nearbyVM.routeCardData) { _ in
                if case let .stopDetails(stopId: _, stopFilter: filter, tripFilter: _) = lastNavEntry, let stopMapData {
                    updateStopDetailsLayers(stopMapData, filter, nearbyVM.routeCardData)
                }
            }
            .onChange(of: mapVM.selectedVehicle) { [weak previousVehicle = mapVM.selectedVehicle] nextVehicle in
                handleSelectedVehicleChange(previousVehicle, nextVehicle)
            }
            .onChange(of: globalMapData) { _ in
                updateGlobalMapDataSources()
            }
            .onDisappear {
                leaveVehiclesChannel()
                viewportProvider.saveCurrentViewport()
            }
            .withScenePhaseHandlers(
                onActive: {
                    if let lastNavEntry {
                        joinVehiclesChannel(navStackEntry: lastNavEntry)
                    }
                },
                onInactive: leaveVehiclesChannel,
                onBackground: leaveVehiclesChannel
            )
            .task {
                while !Task.isCancelled {
                    now = Date.now
                    handleGlobalMapDataChange(now: now)
                    try? await Task.sleep(for: .seconds(30))
                }
            }
    }

    @ViewBuilder
    var staticResponsiveMap: some View {
        ProxyModifiedMap(
            mapContent: AnyView(annotatedMap),
            handleAppear: handleAppear,
            handleTryLayerInit: handleTryLayerInit,
            handleAccessTokenLoaded: handleAccessTokenLoaded,
            globalMapData: globalMapData
        )
        .onChange(of: mapVM.globalData) { _ in
            handleGlobalMapDataChange(now: now)
        }
        .onChange(of: locationDataManager.authorizationStatus) { status in
            guard status == .authorizedAlways || status == .authorizedWhenInUse,
                  viewportProvider.isDefault() else { return }
            viewportProvider.follow(animation: .easeInOut(duration: 0))
            mapVM.layerManager?.resetPuckPosition()
        }
        .onChange(of: nearbyVM.navigationStack) { nextNavStack in
            handleNavStackChange(navigationStack: nextNavStack)
        }
    }

    @ViewBuilder
    var annotatedMap: some View {
        let nav = nearbyVM.navigationStack.last
        let selectedVehicle: Vehicle? = if case .stopDetails = nav {
            mapVM.selectedVehicle
        } else { nil }
        let vehicles: [Vehicle]? = vehiclesData?.filter { $0.id != selectedVehicle?.id }
        AnnotatedMap(
            stopMapData: stopMapData,
            filter: nearbyVM.navigationStack.lastStopDetailsFilter,
            targetedLocation: shouldShowLoadedLocation ? nearbyVM.lastLoadedLocation : nil,
            globalData: mapVM.globalData,
            selectedVehicle: selectedVehicle,
            sheetHeight: sheetHeight,
            vehicles: vehicles,
            handleCameraChange: handleCameraChange,
            handleStyleLoaded: refreshMap,
            handleTapStopLayer: handleTapStopLayer,
            handleTapVehicle: handleTapVehicle,
            viewportProvider: viewportProvider
        )
    }

    private var crosshairs: some View {
        VStack {
            Image(.mapNearbyLocationCursor)
            Spacer()
                .frame(height: sheetHeight - 20)
        }
    }

    var didAppear: ((Self) -> Void)?
}

struct ProxyModifiedMap: View {
    var mapContent: AnyView
    var handleAppear: (_ location: LocationManager?, _ map: MapboxMap?) -> Void
    var handleTryLayerInit: (_ map: MapboxMap?) -> Void
    var handleAccessTokenLoaded: (_ map: MapboxMap?) -> Void
    var globalData: GlobalResponse?
    var railRouteShapes: MapFriendlyRouteResponse?
    var globalMapData: GlobalMapData?

    var body: some View {
        MapReader { proxy in
            mapContent
                .onAppear {
                    handleAppear(proxy.location, proxy.map)
                }
                .onChange(of: globalData) { _ in
                    handleTryLayerInit(proxy.map)
                }
                .onChange(of: railRouteShapes) { _ in
                    handleTryLayerInit(proxy.map)
                }
                .onChange(of: globalMapData) { _ in
                    handleTryLayerInit(proxy.map)
                }
                .onChange(of: MapboxOptions.accessToken) { _ in
                    handleAccessTokenLoaded(proxy.map)
                }
        }
    }
}
