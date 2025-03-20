//
//  HomeMapViewHandlerExtension.swift
//  iosApp
//
//  Created by Simon, Emma on 5/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@_spi(Experimental) import MapboxMaps
import Shared
import SwiftUI

/*
 Functions for handling interactions with the map, like prop change, navigation, and tapping.
 */
extension HomeMapView {
    func handleAppear(location: LocationManager?, map: MapboxMap?) {
        lastNavEntry = nearbyVM.navigationStack.last
        handleTryLayerInit(map: map)

        // Set MapBox to use the current location to display puck
        location?.override(locationProvider: locationDataManager.$currentLocation.map {
            if let location = $0 {
                [Location(clLocation: location)]
            } else { [] }
        }.eraseToSignal())

        // If location data is provided, follow the user's location
        if locationDataManager.currentLocation != nil, viewportProvider.isDefault() {
            viewportProvider.follow(animation: .easeInOut(duration: .zero))
        }

        didAppear?(self)
    }

    func handleGlobalMapDataChange(now: Date) {
        guard let globalData = mapVM.globalData else { return }

        Task(priority: .high) {
            let newMapData = GlobalMapData(
                globalData: globalData,
                alertsByStop: GlobalMapData.companion.getAlertsByStop(
                    globalData: globalData,
                    alerts: nearbyVM.alerts,
                    filterAtTime: now.toKotlinInstant()
                )
            )
            DispatchQueue.main.async { globalMapData = newMapData }
        }
    }

    func handleCameraChange(_ change: CameraChanged) {
        viewportProvider.updateCameraState(change.cameraState)
    }

    func handleNavStackChange(navigationStack: [SheetNavigationStackEntry]) {
        if let filter = navigationStack.lastStopDetailsFilter {
            joinVehiclesChannel(routeId: filter.routeId, directionId: filter.directionId)
        } else {
            leaveVehiclesChannel()
            vehiclesData = []
        }

        lastNavEntry = navigationStack.last
    }

    func checkOnboardingLoaded() {
        if contentVM.onboardingScreensPending == [] {
            locationDataManager.requestWhenInUseAuthorization()
        }
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            mapVM.globalData = globalData
        }
    }

    func fetchGlobalData() {
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "HomeMapView.loadGlobalData",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: fetchGlobalData
            )
        }
    }

    func loadGlobalData() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        fetchGlobalData()
    }

    @MainActor
    func activateRouteShapeListener() async {
        for await railRouteShapes in railRouteShapeRepository.state {
            self.railRouteShapes = railRouteShapes
        }
    }

    func fetchRouteShapes() {
        Task {
            await fetchApi(
                errorBannerRepository,
                errorKey: "HomeMapView.handleAppear",
                getData: { try await railRouteShapeRepository.getRailRouteShapes() },
                onRefreshAfterError: fetchRouteShapes
            )
        }
    }

    func loadRouteShapes() {
        Task(priority: .high) {
            await activateRouteShapeListener()
        }
        fetchRouteShapes()
    }

    func joinVehiclesChannel(navStackEntry entry: SheetNavigationStackEntry) {
        if case let .stopDetails(stopId: _, stopFilter: stopFilter, tripFilter: _) = entry, let stopFilter {
            joinVehiclesChannel(
                routeId: stopFilter.routeId,
                directionId: stopFilter.directionId
            )
        }
    }

    func joinVehiclesChannel(routeId: String, directionId: Int32) {
        leaveVehiclesChannel()
        vehiclesRepository.connect(routeId: routeId, directionId: directionId) { outcome in
            if case let .ok(result) = onEnum(of: outcome) {
                if let departures = nearbyVM.departures {
                    vehiclesData = Array(departures.filterVehiclesByUpcoming(vehicles: result.data).values)
                }
            }
        }
    }

    func leaveVehiclesChannel() {
        vehiclesRepository.disconnect()
    }

    func handleLastNavChange(oldNavEntry: SheetNavigationStackEntry?, nextNavEntry: SheetNavigationStackEntry?) {
        if oldNavEntry == nil {
            viewportProvider.saveNearbyTransitViewport()
        }
        if case let .stopDetails(stopId, stopFilter, _) = nextNavEntry {
            if oldNavEntry?.stopId() != stopId {
                if let stop = mapVM.globalData?.getStop(stopId: stopId) {
                    handleStopDetailsChange(stop, stopFilter)
                }
            }

            handleRouteFilterChange(stopFilter)
        }

        if nextNavEntry == nil {
            clearSelectedStop()
            viewportProvider.restoreNearbyTransitViewport()
        }
    }

    func handleStopDetailsChange(_ stop: Stop, _ filter: StopDetailsFilter?) {
        mapVM.stopSourceData = .init(selectedStopId: stop.id)
        viewportProvider.animateTo(coordinates: stop.coordinate)

        Task {
            if case let .ok(result) = try await onEnum(of: stopRepository.getStopMapData(stopId: stop.id)) {
                stopMapData = result.data
                if let stopMapData {
                    updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
                }
            }
        }
    }

    func handleRouteFilterChange(_ filter: StopDetailsFilter?) {
        if let stopMapData {
            updateStopDetailsLayers(stopMapData, filter, nearbyVM.departures)
        }
    }

    func clearSelectedStop() {
        stopMapData = nil
        resetDefaultSources()
    }

    func handleTapStopLayer(feature: QueriedFeature, _: MapContentGestureContext) -> Bool {
        guard case let .string(stopId) = feature.feature.properties?[StopFeaturesBuilder.shared.propIdKey.key] else {
            let featureId = feature.feature.identifier.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped, but had invalid stop id prop. sourceId=\(feature.source)
            """)
            return false
        }
        guard let stop = mapVM.globalData?.getStop(stopId: stopId) else {
            let featureId = feature.feature.identifier.debugDescription
            log.error("""
                Stop icon featureId=`\(featureId)` was tapped but stopId=\(stopId) didn't exist in global stops.
            """)
            return false
        }
        analytics.tappedOnStop(stopId: stop.id)
        nearbyVM.navigationStack.removeAll()
        nearbyVM.pushNavEntry(.stopDetails(stopId: stop.id, stopFilter: nil, tripFilter: nil))
        return true
    }

    func handleTapVehicle(_ vehicle: Vehicle) {
        guard let tripId = vehicle.tripId else { return }
        guard case .stopDetails = nearbyVM.navigationStack.lastSafe() else { return }

        let departures = nearbyVM.departures
        let patterns = departures?.routes.first(where: { patterns in
            patterns.routes.contains { $0.id == vehicle.routeId }
        })
        let trip = patterns?.allUpcomingTrips().first(where: { upcoming in
            upcoming.trip.id == tripId
        })
        let stopSequence = trip?.stopSequence
        let routeId = trip?.trip.routeId ?? vehicle.routeId ?? patterns?.routeIdentifier

        if let routeId { analytics.tappedVehicle(routeId: routeId) }

        nearbyVM.navigationStack.lastTripDetailsFilter = .init(
            tripId: tripId,
            vehicleId: vehicle.id,
            stopSequence: stopSequence,
            selectionLock: true
        )
    }

    func handleSelectedVehicleChange(_ previousVehicle: Vehicle?, _ nextVehicle: Vehicle?) {
        guard let globalData = mapVM.globalData else { return }
        guard let stop = nearbyVM.getTargetStop(global: globalData) else { return }
        guard let nextVehicle else {
            viewportProvider.animateTo(coordinates: stop.coordinate)
            return
        }
        if previousVehicle == nil || previousVehicle?.id != nextVehicle.id {
            viewportProvider.vehicleOverview(vehicle: nextVehicle, stop: stop)
        }
    }
}
