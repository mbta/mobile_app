//
//  TripDetailsView.swift
//  iosApp
//
//  Created by esimon on 11/14/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import shared
import SwiftPhoenixClient
import SwiftUI

struct TripDetailsView: View {
    let tripId: String
    let vehicleId: String?
    let routeId: String
    let stopId: String
    let stopSequence: Int?

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    let analytics: TripDetailsAnalytics
    let inspection = Inspection<Self>()

    private var route: Route? {
        let trip: Trip? = stopDetailsVM.tripPredictions?.trips[tripId]
        let vehicle: Vehicle? = stopDetailsVM.vehicle
        return if let routeId = trip?.routeId ?? vehicle?.routeId {
            stopDetailsVM.global?.routes[routeId]
        } else {
            nil
        }
    }

    init(
        tripId: String,
        vehicleId: String?,
        routeId: String,
        stopId: String,
        stopSequence: Int?,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel,

        analytics: TripDetailsAnalytics = AnalyticsProvider.shared
    ) {
        self.tripId = tripId
        self.vehicleId = vehicleId
        self.routeId = routeId
        self.stopId = stopId
        self.stopSequence = stopSequence
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM

        self.analytics = analytics
    }

    var body: some View {
        VStack(spacing: 16) {
            if nearbyVM.showDebugMessages {
                DebugView {
                    VStack {
                        Text(verbatim: "trip id: \(tripId)")
                        Text(verbatim: "vehicle id: \(vehicleId ?? "nil")")
                    }
                }
            }
            if stopDetailsVM.tripPredictionsLoaded,
               let global = stopDetailsVM.global,
               let vehicle = stopDetailsVM.vehicle,
               let stops = TripDetailsStopList.companion.fromPieces(
                   tripId: tripId,
                   directionId: stopDetailsVM.trip?.directionId ?? vehicle.directionId,
                   tripSchedules: stopDetailsVM.tripSchedules,
                   tripPredictions: stopDetailsVM.tripPredictions,
                   vehicle: vehicle,
                   alertsData: nearbyVM.alerts,
                   globalData: global
               ) {
                let vehicleStop: Stop? = if let stopId = vehicle.stopId, let allStops = stopDetailsVM.global?.stops {
                    allStops[stopId]?.resolveParent(stops: allStops)
                } else {
                    nil
                }
                tripDetails(stops, vehicle, vehicleStop, route)
            } else {
                loadingBody()
            }
        }
        .padding(.horizontal, 6)
        .task { stopDetailsVM.loadTripDetails(tripId: tripId) }
        .onAppear { joinRealtime() }
        .onDisappear {
            stopDetailsVM.clearTripDetails()
            clearMapVehicle()
        }
        .onChange(of: tripId) { nextTripId in
            mapVM.selectedVehicle = nil
            stopDetailsVM.clearTripDetails()
            stopDetailsVM.loadTripDetails(tripId: nextTripId)
            stopDetailsVM.joinTripPredictions(tripId: nextTripId)
        }
        .onChange(of: vehicleId) { vehicleId in
            leaveVehicle()
            joinVehicle(vehicleId: vehicleId)
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .withScenePhaseHandlers(
            onActive: {
                stopDetailsVM.returnFromBackground()
                joinRealtime()
            },
            onInactive: leaveRealtime,
            onBackground: leaveRealtime
        )
    }

    var didLoadData: ((Self) -> Void)?

    @ViewBuilder private func tripDetails(
        _ stops: TripDetailsStopList,
        _ vehicle: Vehicle?,
        _ vehicleStop: Stop?,
        _ route: Route?
    ) -> some View {
        let vehicleShown = vehicle != nil && route != nil && vehicleStop != nil
        ZStack(alignment: .top) {
            VStack(spacing: 0) {
                // Dummy vehicle card to space the stop list down exactly the height of the card
                vehicleCardView(vehicle, vehicleStop, route)
                TripStops(
                    targetId: stopId,
                    stops: stops,
                    stopSequence: stopSequence,
                    vehicleShown: vehicleShown,
                    now: now,
                    onTapLink: onTapStop,
                    route: route,
                    global: stopDetailsVM.global
                )
                .padding(.top, vehicleShown ? -56 : 0)
            }
            vehicleCardView(vehicle, vehicleStop, route)
        }
    }

    @ViewBuilder
    func vehicleCardView(
        _ vehicle: Vehicle?,
        _ vehicleStop: Stop?,
        _ route: Route?
    ) -> some View {
        if let vehicle, let route, let vehicleStop {
            TripVehicleCard(
                vehicle: vehicle,
                route: route,
                stop: vehicleStop,
                tripId: tripId
            )
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderInfo = LoadingPlaceholders.shared.tripDetailsInfo()
        tripDetails(
            placeholderInfo.stops,
            placeholderInfo.vehicle,
            placeholderInfo.vehicleStop,
            placeholderInfo.route
        ).loadingPlaceholder()
    }

    private func clearMapVehicle() {
        if mapVM.selectedVehicle?.id == vehicleId {
            mapVM.selectedVehicle = nil
        }
    }

    private func joinRealtime() {
        stopDetailsVM.joinTripPredictions(tripId: tripId)
        joinVehicle(vehicleId: vehicleId)
    }

    private func joinVehicle(vehicleId: String?) {
        stopDetailsVM.joinVehicle(
            tripId: tripId,
            vehicleId: vehicleId,
            onSuccess: { vehicle in mapVM.selectedVehicle = vehicle }
        )
    }

    private func leaveRealtime() {
        stopDetailsVM.leaveTripPredictions()
        leaveVehicle()
    }

    private func leaveVehicle() {
        stopDetailsVM.leaveVehicle()
        clearMapVehicle()
    }

    func onTapStop(
        entry: SheetNavigationStackEntry,
        stop: TripDetailsStopList.Entry,
        connectingRouteId: String?
    ) {
        // resolve parent stop before following link
        let realEntry = switch entry {
        case let .legacyStopDetails(stop, filter): SheetNavigationStackEntry.legacyStopDetails(
                stop.resolveParent(stops: stopDetailsVM.global?.stops ?? [:]),
                filter
            )
        default: entry
        }
        nearbyVM.pushNavEntry(realEntry)
        analytics.tappedDownstreamStop(
            routeId: stopDetailsVM.trip?.routeId ?? "",
            stopId: stop.stop.id,
            tripId: tripId,
            connectingRouteId: connectingRouteId
        )
    }
}
