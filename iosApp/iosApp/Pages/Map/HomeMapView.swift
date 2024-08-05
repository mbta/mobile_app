//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct HomeMapView: View {
    var analytics: NearbyTransitAnalytics = AnalyticsProvider.shared
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var vehiclesFetcher: VehiclesFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @Environment(\.colorScheme) var colorScheme

    var globalRepository: IGlobalRepository
    @State var globalData: GlobalResponse?

    var stopRepository: IStopRepository
    @State var globalMapData: GlobalMapData?
    @State var stopMapData: StopMapResponse?
    @State var upcomingRoutePatterns: Set<String> = .init()

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
            && nearbyVM.navigationStack.isEmpty
    }

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        mapVM: MapViewModel,
        nearbyVM: NearbyViewModel,
        railRouteShapeFetcher: RailRouteShapeFetcher,
        vehiclesFetcher: VehiclesFetcher,
        viewportProvider: ViewportProvider,
        stopRepository: IStopRepository = RepositoryDI().stop,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
        sheetHeight: Binding<CGFloat>,
        globalMapData: GlobalMapData? = nil
    ) {
        self.globalRepository = globalRepository
        self.mapVM = mapVM
        self.nearbyVM = nearbyVM
        self.railRouteShapeFetcher = railRouteShapeFetcher
        self.vehiclesFetcher = vehiclesFetcher
        self.viewportProvider = viewportProvider
        self.stopRepository = stopRepository
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
        _sheetHeight = sheetHeight
        _globalMapData = State(wrappedValue: globalMapData)
    }

    var body: some View {
        realtimeResponsiveMap
            .overlay(alignment: .topTrailing) {
                if !viewportProvider.viewport.isFollowing, locationDataManager.currentLocation != nil {
                    RecenterButton { Task { viewportProvider.follow() } }
                }
            }
            .overlay(alignment: .center) {
                if nearbyVM.selectingLocation {
                    crosshairs
                }
            }
            .task {
                do {
                    globalData = try await globalRepository.getGlobalData()
                } catch {
                    debugPrint(error)
                }
            }
            .onChange(of: lastNavEntry) { [oldNavEntry = lastNavEntry] nextNavEntry in
                handleLastNavChange(oldNavEntry: oldNavEntry, nextNavEntry: nextNavEntry)
            }
            .onChange(of: mapVM.routeSourceData) { routeData in
                updateRouteSources(routeData: routeData)
            }
            .onChange(of: mapVM.stopSourceData) { stopData in
                updateStopSource(stopData: stopData)
            }
            .onChange(of: mapVM.childStops) { childStops in
                updateChildStopSource(childStops: childStops)
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .onChange(of: viewportProvider.isManuallyCentering) { isManuallyCentering in
                guard isManuallyCentering else { return }
                /*
                 This will be set to false after nearby is loaded to avoid the crosshair
                 dissapearing and re-appearing
                 */
                nearbyVM.selectingLocation = true
            }
            .withScenePhaseHandlers(onActive: {
                // Layers are removed when the app is backgrounded, add them back.
                refreshLayers()
            })
    }

    @ViewBuilder
    var realtimeResponsiveMap: some View {
        staticResponsiveMap
            .onChange(of: nearbyVM.alerts) { _ in
                handleGlobalMapDataChange(now: now)
            }
            .onChange(of: nearbyVM.departures) { _ in
                if case let .stopDetails(_, filter) = lastNavEntry, let stopMapData {
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
                vehiclesFetcher.leave()
            }
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
            railRouteShapeFetcher: railRouteShapeFetcher,
            globalMapData: globalMapData
        )
        .onChange(of: globalData) { _ in
            handleGlobalMapDataChange(now: now)
        }
        .onChange(of: locationDataManager.authorizationStatus) { status in
            guard status == .authorizedAlways || status == .authorizedWhenInUse else { return }
            viewportProvider.follow(animation: .easeInOut(duration: 0))
        }
        .onChange(of: nearbyVM.navigationStack) { nextNavStack in
            handleNavStackChange(navigationStack: nextNavStack)
        }
    }

    @ViewBuilder
    var annotatedMap: some View {
        let selectedVehicle: Vehicle? = if case .tripDetails = nearbyVM.navigationStack.last {
            mapVM.selectedVehicle
        } else { nil }
        let vehicles: [Vehicle]? = vehiclesFetcher.vehicles?.filter { $0.id != selectedVehicle?.id }
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
            handleMapLoaded: refreshLayers,
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
    var globalData: GlobalResponse?
    var railRouteShapeFetcher: RailRouteShapeFetcher
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
                .onChange(of: railRouteShapeFetcher.response) { _ in
                    handleTryLayerInit(proxy.map)
                }
                .onChange(of: globalMapData) { _ in
                    handleTryLayerInit(proxy.map)
                }
        }
    }
}
