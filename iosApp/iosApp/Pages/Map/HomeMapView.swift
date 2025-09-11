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
    var mapVM: IMapViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    var routeCardDataVM: IRouteCardDataViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @Environment(\.colorScheme) var colorScheme

    var errorBannerRepository: IErrorBannerStateRepository

    @State var vehiclesRepository: IVehiclesRepository
    @State var vehiclesData: [Vehicle]?

    @StateObject var locationDataManager: LocationDataManager
    @Binding var sheetHeight: CGFloat

    @State var mapVMState: MapViewModel.State = MapViewModel.StateOverview.shared
    @State var globalData: GlobalResponse?
    @Binding var selectedVehicle: Vehicle?
    @State var routeCardDataState: RouteCardDataViewModel.State?

    let inspection = Inspection<Self>()
    let log = Logger()

    private var shouldShowLoadedLocation: Bool {
        !viewportProvider.viewport.isFollowing
            && nearbyVM.navigationStack.lastSafe().allowTargeting
            && !nearbyVM.isTargeting
    }

    init(
        contentVM: ContentViewModel,
        mapVM: IMapViewModel,
        nearbyVM: NearbyViewModel,
        routeCardDataVM: IRouteCardDataViewModel,
        viewportProvider: ViewportProvider,
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        vehiclesData: [Vehicle]? = nil,
        vehiclesRepository: IVehiclesRepository = RepositoryDI().vehicles,
        locationDataManager: LocationDataManager,
        sheetHeight: Binding<CGFloat>,
        globalMapData _: GlobalMapData? = nil,
        selectedVehicle: Binding<Vehicle?>
    ) {
        self.contentVM = contentVM
        self.mapVM = mapVM
        self.nearbyVM = nearbyVM
        self.routeCardDataVM = routeCardDataVM
        self.viewportProvider = viewportProvider
        self.errorBannerRepository = errorBannerRepository
        self.vehiclesData = vehiclesData
        self.vehiclesRepository = vehiclesRepository
        _locationDataManager = StateObject(wrappedValue: locationDataManager)
        _sheetHeight = sheetHeight
        _selectedVehicle = selectedVehicle
    }

    var body: some View {
        realtimeResponsiveMap
            .overlay(alignment: .center) {
                if nearbyVM.isTargeting {
                    crosshairs
                }
            }
            .global($globalData, errorKey: "HomeMapView")
            .task {
                for await state in mapVM.models {
                    mapVMState = state
                }
            }
            .onChange(of: mapVMState) { state in
                let filteredVehicle = nearbyVM.navigationStack.lastTripDetailsFilter?.vehicleId
                if case let .tripSelected(tripState) = onEnum(of: state),
                   filteredVehicle == tripState.vehicle?.id {
                    selectedVehicle = tripState.vehicle
                } else {
                    selectedVehicle = nil
                }
            }
            .onChange(of: nearbyVM.navigationStack) { navStack in
                Task {
                    let currentNavEntry = navStack.lastSafe().toSheetRoute()
                    mapVM.navChanged(currentNavEntry: currentNavEntry)
                    handleNavStackChange(navigationStack: navStack)
                }
            }
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
            .onChange(of: colorScheme) { newColorScheme in
                mapVM.colorPaletteChanged(isDarkMode: newColorScheme == .dark)
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    @ViewBuilder
    var realtimeResponsiveMap: some View {
        staticResponsiveMap
            .manageVM(routeCardDataVM, $routeCardDataState)
            .onChange(of: nearbyVM.alerts) { _ in
                mapVM.alertsChanged(alerts: nearbyVM.alerts)
            }
            .onDisappear {
                leaveVehiclesChannel()
                viewportProvider.saveCurrentViewport()
            }
            .withScenePhaseHandlers(
                onActive: {
                    let lastNavEntry = nearbyVM.navigationStack.lastSafe()
                    joinVehiclesChannel(navStackEntry: lastNavEntry)
                },
                onInactive: leaveVehiclesChannel,
                onBackground: leaveVehiclesChannel
            )
    }

    @ViewBuilder
    var staticResponsiveMap: some View {
        ProxyModifiedMap(
            mapContent: AnyView(annotatedMap),
            handleAppear: handleAppear,
            handleAccessTokenLoaded: handleAccessTokenLoaded,
        )
        .onChange(of: locationDataManager.authorizationStatus) { status in
            Task {
                guard status == .authorizedAlways || status == .authorizedWhenInUse,
                      viewportProvider.isDefault() else { return }
                mapVM.locationPermissionsChanged(hasPermission: true)
            }
        }
    }

    @ViewBuilder
    var annotatedMap: some View {
        let nav = nearbyVM.navigationStack.last
        let vehicles: [Vehicle]? = vehiclesData?.filter { $0.id != selectedVehicle?.id }
        AnnotatedMap(
            filter: nearbyVM.navigationStack.lastStopDetailsFilter,
            targetedLocation: shouldShowLoadedLocation ? nearbyVM.lastLoadedLocation : nil,
            globalData: globalData,
            selectedVehicle: selectedVehicle,
            sheetHeight: sheetHeight,
            vehicles: vehicles,
            handleCameraChange: handleCameraChange,
            handleStyleLoaded: { mapVM.mapStyleLoaded() },
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
    var handleAccessTokenLoaded: (_ map: MapboxMap?) -> Void

    var body: some View {
        MapReader { proxy in
            mapContent
                .onAppear {
                    handleAppear(proxy.location, proxy.map)
                }
                .onChange(of: MapboxOptions.accessToken) { _ in
                    handleAccessTokenLoaded(proxy.map)
                }
        }
    }
}
