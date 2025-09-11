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
    @State var mapVM: Shared.IMapViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @Environment(\.colorScheme) var colorScheme

    var errorBannerRepository: IErrorBannerStateRepository

    @State var vehiclesRepository: IVehiclesRepository
    @State var vehiclesData: [Vehicle]?

    @StateObject var locationDataManager: LocationDataManager
    @Binding var sheetHeight: CGFloat

    @State var mapVMState: Shared.MapViewModel.State = Shared.MapViewModel.StateOverview.shared
    @State var globalData: GlobalResponse?
    @Binding var selectedVehicle: Vehicle?

    let inspection = Inspection<Self>()
    let log = Logger()

    private var shouldShowLoadedLocation: Bool {
        !viewportProvider.viewport.isFollowing
            && nearbyVM.navigationStack.lastSafe().allowTargeting
            && !nearbyVM.isTargeting
    }

    init(
        contentVM: ContentViewModel,
        mapVM: Shared.IMapViewModel,
        nearbyVM: NearbyViewModel,
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
            .onChange(of: nearbyVM.navigationStack) { navStack in
                Task {
                    let currentNavEntry = navStack.lastSafe().toSheetRoute()
                    mapVM.navChanged(currentNavEntry: currentNavEntry)
                    handleNavStackChange(navigationStack: navStack)
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
            .task {
                for await state in mapVM.models {
                    mapVMState = state
                }
            }
            .onChange(of: colorScheme) { newColorScheme in
                mapVM.colorPaletteChanged(isDarkMode: newColorScheme == .dark)
            }
            .onChange(of: mapVMState) { state in
                if case let .tripSelected(tripState) = onEnum(of: state) {
                    selectedVehicle = tripState.vehicle
                } else {
                    selectedVehicle = nil
                }
            }
    }

    @ViewBuilder
    var realtimeResponsiveMap: some View {
        staticResponsiveMap
            .onChange(of: nearbyVM.alerts) { _ in
                mapVM.alertsChanged(alerts: nearbyVM.alerts)
            }
            .onChange(of: nearbyVM.routeCardData) { _ in
                mapVM.routeCardDataChanged(routeCardData: nearbyVM.routeCardData)
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
