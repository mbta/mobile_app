//
//  HomeMapView.swift
//  iosApp
//
//  Created by Brady, Kayla on 2/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import os
import Polyline
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct HomeMapView: View {
    @ObservedObject var alertsFetcher: AlertsFetcher
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var railRouteShapeFetcher: RailRouteShapeFetcher
    @ObservedObject var vehiclesFetcher: VehiclesFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    var stopRepository: IStopRepository
    @State var stopMapData: StopMapResponse?
    @State var upcomingRoutePatterns: Set<String> = .init()

    @StateObject var locationDataManager: LocationDataManager
    @Binding var sheetHeight: CGFloat

    @State var layerManager: IMapLayerManager?
    @State private var recenterButton: ViewAnnotation?
    @State private var now = Date.now
    @State var currentStopAlerts: [String: AlertAssociatedStop] = [:]
    @State var routesByStop: [String: [Route]] = [:]
    @State var lastNavEntry: SheetNavigationStackEntry?

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 300, on: .main, in: .common).autoconnect()
    let log = Logger()

    private var isNearbyNotFollowing: Bool {
        !viewportProvider.viewport.isFollowing && locationDataManager.currentLocation != nil
            && nearbyVM.navigationStack.isEmpty
    }

    init(
        alertsFetcher: AlertsFetcher,
        globalFetcher: GlobalFetcher,
        nearbyFetcher: NearbyFetcher,
        nearbyVM: NearbyViewModel,
        railRouteShapeFetcher: RailRouteShapeFetcher,
        vehiclesFetcher: VehiclesFetcher,
        viewportProvider: ViewportProvider,
        stopRepository: IStopRepository = RepositoryDI().stop,
        locationDataManager: LocationDataManager = .init(distanceFilter: 1),
        sheetHeight: Binding<CGFloat>,
        layerManager: IMapLayerManager? = nil
    ) {
        self.alertsFetcher = alertsFetcher
        self.globalFetcher = globalFetcher
        self.nearbyFetcher = nearbyFetcher
        self.nearbyVM = nearbyVM
        self.railRouteShapeFetcher = railRouteShapeFetcher
        self.vehiclesFetcher = vehiclesFetcher
        self.viewportProvider = viewportProvider
        self.stopRepository = stopRepository
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
        _sheetHeight = sheetHeight
        _layerManager = State(wrappedValue: layerManager)
    }

    var body: some View {
        modifiedMap.overlay(alignment: .topTrailing) {
            if !viewportProvider.viewport.isFollowing, locationDataManager.currentLocation != nil {
                RecenterButton { Task { viewportProvider.follow() } }
            }
        }
    }

    @ViewBuilder
    var modifiedMap: some View {
        ProxyModifiedMap(
            mapContent: AnyView(annotatedMap),
            handleAppear: handleAppear,
            handleTryLayerInit: handleTryLayerInit,
            globalFetcher: globalFetcher,
            railRouteShapeFetcher: railRouteShapeFetcher
        )
        .onChange(of: alertsFetcher.alerts) { nextAlerts in
            currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                alerts: nextAlerts,
                filterAtTime: now.toKotlinInstant()
            )
        }
        .onChange(of: nearbyVM.departures) { _ in
            if case let .stopDetails(_, filter) = lastNavEntry, let stopMapData {
                updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
            }
        }
        .onChange(of: currentStopAlerts) { nextStopAlerts in
            handleStopAlertChange(alertsByStop: nextStopAlerts)
        }
        .onChange(of: globalFetcher.response) { _ in
            currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                alerts: alertsFetcher.alerts,
                filterAtTime: now.toKotlinInstant()
            )
            var routes: [Route] = []
            if let red = globalFetcher.routes["Red"], let green = globalFetcher.routes["Green-C"] {
                routes.append(red)
                routes.append(green)
                print(green)
            }
            routesByStop = [
                "place-pktrm": routes,
            ]
        }
        .onChange(of: locationDataManager.authorizationStatus) { status in
            guard status == .authorizedAlways || status == .authorizedWhenInUse else { return }
            viewportProvider.follow(animation: .easeInOut(duration: 0))
        }
        .onChange(of: nearbyVM.navigationStack) { nextNavStack in
            handleNavStackChange(navigationStack: nextNavStack)
        }
        .onChange(of: lastNavEntry) { [oldNavEntry = lastNavEntry] nextNavEntry in
            handleLastNavChange(oldNavEntry: oldNavEntry, nextNavEntry: nextNavEntry)
        }
        .onDisappear {
            vehiclesFetcher.leave()
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onReceive(timer) { input in
            now = input
            currentStopAlerts = globalFetcher.getRealtimeAlertsByStop(
                alerts: alertsFetcher.alerts,
                filterAtTime: now.toKotlinInstant()
            )
        }
    }

    @ViewBuilder
    var annotatedMap: some View {
        AnnotatedMap(
            stopMapData: stopMapData,
            filter: nearbyVM.navigationStack.lastStopDetailsFilter,
            nearbyLocation: isNearbyNotFollowing ? nearbyFetcher.loadedLocation : nil,
            sheetHeight: sheetHeight,
            vehicles: vehiclesFetcher.vehicles,
            viewportProvider: viewportProvider,
            handleCameraChange: handleCameraChange,
            handleTapStopLayer: handleTapStopLayer,
            handleTapVehicle: handleTapVehicle
        )
    }

    var didAppear: ((Self) -> Void)?
}

struct ProxyModifiedMap: View {
    var mapContent: AnyView
    var handleAppear: (_ location: LocationManager?, _ map: MapboxMap?) -> Void
    var handleTryLayerInit: (_ map: MapboxMap?) -> Void
    var globalFetcher: GlobalFetcher
    var railRouteShapeFetcher: RailRouteShapeFetcher

    var body: some View {
        MapReader { proxy in
            mapContent
                .onAppear {
                    handleAppear(proxy.location, proxy.map)
                }
                .onChange(of: globalFetcher.response) { _ in
                    handleTryLayerInit(proxy.map)
                }
                .onChange(of: railRouteShapeFetcher.response) { _ in
                    handleTryLayerInit(proxy.map)
                }
        }
    }
}
