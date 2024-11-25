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
import shared
import SwiftUI

struct HomeMapView: View {
    var analytics: NearbyTransitAnalytics = AnalyticsProvider.shared
    @ObservedObject var contentVM: ContentViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @Environment(\.colorScheme) var colorScheme

    var errorBannerRepository: IErrorBannerStateRepository

    var globalRepository: IGlobalRepository
    @State var globalData: GlobalResponse?

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
    let timer = Timer.publish(every: 300, on: .main, in: .common).autoconnect()
    let log = Logger()

    private var isNearbyNotFollowing: Bool {
        !viewportProvider.viewport.isFollowing && locationDataManager.currentLocation != nil
            && nearbyVM.navigationStack.lastSafe() == .nearby
    }

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        contentVM: ContentViewModel,
        mapVM: MapViewModel,
        nearbyVM: NearbyViewModel,
        viewportProvider: ViewportProvider,
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        railRouteShapeRepository: IRailRouteShapeRepository = RepositoryDI().railRouteShapes,
        stopRepository: IStopRepository = RepositoryDI().stop,
        vehiclesData: [Vehicle]? = nil,
        vehiclesRepository: IVehiclesRepository = RepositoryDI().vehicles,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
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
                if nearbyVM.selectingLocation {
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
            }
            .onChange(of: mapVM.stopSourceData) { _ in
                updateStopSource()
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .onChange(of: viewportProvider.isManuallyCentering) { isManuallyCentering in
                guard isManuallyCentering, nearbyVM.navigationStack.lastSafe() == .nearby else { return }
                /*
                 This will be set to false after nearby is loaded to avoid the crosshair
                 dissapearing and re-appearing
                 */
                nearbyVM.selectingLocation = true
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
            .onChange(of: nearbyVM.departures) { _ in
                if case let .stopDetails(stopId: _, stopFilter: filter, tripFilter: _) = lastNavEntry, let stopMapData {
                    updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
                }
                if case let .legacyStopDetails(_, filter) = lastNavEntry, let stopMapData {
                    updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
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
            .onReceive(timer) { input in
                now = input
                handleGlobalMapDataChange(now: now)
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
        .onChange(of: globalData) { _ in
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
        let selectedVehicle: Vehicle? = if case .tripDetails = nav {
            mapVM.selectedVehicle
        } else if case .stopDetails = nav {
            mapVM.selectedVehicle
        } else { nil }
        let vehicles: [Vehicle]? = vehiclesData?.filter { $0.id != selectedVehicle?.id }
        AnnotatedMap(
            stopMapData: stopMapData,
            filter: nearbyVM.navigationStack.lastStopDetailsFilter,
            nearbyLocation: isNearbyNotFollowing ? nearbyVM.nearbyState.loadedLocation : nil,
            routes: globalData?.routes,
            selectedVehicle: selectedVehicle,
            sheetHeight: sheetHeight,
            vehicles: vehicles,
            viewportProvider: viewportProvider,
            handleCameraChange: handleCameraChange,
            handleStyleLoaded: refreshMap,
            handleTapStopLayer: handleTapStopLayer,
            handleTapVehicle: handleTapVehicle
        )
    }

    private var crosshairs: some View {
        VStack {
            Image("map-nearby-location-cursor")
            Spacer()
                .frame(height: sheetHeight)
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
