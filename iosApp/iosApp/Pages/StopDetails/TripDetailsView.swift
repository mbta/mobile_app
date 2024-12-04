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
    var global: GlobalResponse?

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    let analytics: TripDetailsAnalytics
    let inspection = Inspection<Self>()

    private var routeType: RouteType? {
        global?.routes[routeId]?.type
    }

    init(
        tripId: String,
        vehicleId: String?,
        routeId: String,
        stopId: String,
        stopSequence: Int?,
        now: Date,
        global: GlobalResponse?,
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
        self.global = global
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
                        Text(verbatim: "vehicle id: \(vehicleId)")
                    }
                }
            }
            if stopDetailsVM.tripPredictionsLoaded, let global, let vehicle = stopDetailsVM.vehicle,
               let stops = TripDetailsStopList.companion.fromPieces(
                   tripId: tripId,
                   directionId: stopDetailsVM.trip?.directionId ?? vehicle.directionId,
                   tripSchedules: stopDetailsVM.tripSchedules,
                   tripPredictions: stopDetailsVM.tripPredictions,
                   vehicle: vehicle,
                   alertsData: nearbyVM.alerts,
                   globalData: global
               ) {
                ZStack(alignment: .top) {
                    VStack(spacing: 0) {
                        // Dummy vehicle card to space the stop list down exactly the height of the card
                        vehicleCardView
                        TripStops(
                            targetId: stopId,
                            stops: stops,
                            stopSequence: stopSequence,
                            now: now,
                            onTapLink: onTapStop,
                            routeType: routeType,
                            global: global
                        ).padding(.top, -10)
                    }
                    vehicleCardView
                }
            } else {
                loadingBody()
            }
        }
        .padding(.horizontal, 6)
        .task { stopDetailsVM.loadTripDetails(tripId: tripId) }
        .onAppear { joinRealtime() }
        .onDisappear { leaveRealtime() }
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

    @ViewBuilder private func loadingBody() -> some View {
        TripDetailsStopListView(
            stops: LoadingPlaceholders.shared.tripDetailsStops(),
            now: now.toKotlinInstant(),
            onTapLink: { _, _, _ in },
            routeType: nil
        )
        .loadingPlaceholder()
    }

    private func joinRealtime() {
        stopDetailsVM.joinTripPredictions(tripId: tripId)
        joinVehicle(vehicleId: vehicleId)
    }

    private func leaveRealtime() {
        stopDetailsVM.leaveTripPredictions()
        leaveVehicle()
    }

    private func joinVehicle(vehicleId: String?) {
        stopDetailsVM.joinVehicle(
            tripId: tripId,
            vehicleId: vehicleId,
            onSuccess: { vehicle in mapVM.selectedVehicle = vehicle }
        )
    }

    private func leaveVehicle() {
        stopDetailsVM.leaveVehicle()
        if mapVM.selectedVehicle?.id == vehicleId {
            mapVM.selectedVehicle = nil
        }
    }

    @ViewBuilder
    var vehicleCardView: some View {
        let trip: Trip? = stopDetailsVM.tripPredictions?.trips[tripId]
        let vehicle: Vehicle? = stopDetailsVM.vehicle
        let vehicleStop: Stop? = if let stopId = vehicle?.stopId, let allStops = global?.stops {
            allStops[stopId]?.resolveParent(stops: allStops)
        } else {
            nil
        }
        let route: Route? = if let routeId = trip?.routeId ?? vehicle?.routeId {
            global?.routes[routeId]
        } else {
            nil
        }
        if let vehicle, let route, let vehicleStop {
            TripVehicleCard(
                vehicle: vehicle,
                route: route,
                stop: vehicleStop,
                tripId: tripId
            )
        }
    }

    func onTapStop(
        entry: SheetNavigationStackEntry,
        stop: TripDetailsStopList.Entry,
        connectingRouteId: String?
    ) {
        // resolve parent stop before following link
        let realEntry = switch entry {
        case let .legacyStopDetails(stop, filter): SheetNavigationStackEntry.legacyStopDetails(
                stop.resolveParent(stops: global?.stops ?? [:]),
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
