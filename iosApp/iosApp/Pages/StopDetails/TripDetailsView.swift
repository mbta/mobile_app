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
    var tripFilter: TripDetailsFilter?
    var stopId: String

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    let analytics: TripDetailsAnalytics
    let inspection = Inspection<Self>()

    init(
        tripFilter: TripDetailsFilter?,
        stopId: String,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel,

        analytics: TripDetailsAnalytics = AnalyticsProvider.shared
    ) {
        self.tripFilter = tripFilter
        self.stopId = stopId
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
                        Text(verbatim: "trip id: \(tripFilter?.tripId ?? "nil")")
                        Text(verbatim: "vehicle id: \(tripFilter?.vehicleId ?? "nil")")
                    }
                }
            }
            if let tripFilter,
               let tripData = stopDetailsVM.tripData,
               tripData.tripFilter == tripFilter,
               let global = stopDetailsVM.global,
               let vehicle = stopDetailsVM.tripData?.vehicle,
               let stops = TripDetailsStopList.companion.fromPieces(
                   tripId: tripFilter.tripId,
                   directionId: tripData.trip.directionId,
                   tripSchedules: tripData.tripSchedules,
                   tripPredictions: tripData.tripPredictions,
                   vehicle: vehicle,
                   alertsData: nearbyVM.alerts,
                   globalData: global
               ) {
                let vehicleStop: Stop? = if let stopId = vehicle.stopId, let allStops = stopDetailsVM.global?.stops {
                    allStops[stopId]?.resolveParent(stops: allStops)
                } else {
                    nil
                }
                let routeAccents = stopDetailsVM.getTripRouteAccents()
                tripDetails(tripFilter.tripId, stops, vehicle, vehicleStop, routeAccents)
            } else {
                loadingBody()
            }
        }
        .padding(.horizontal, 6)
        .task { stopDetailsVM.handleTripFilterChange(tripFilter) }
        .onDisappear {
            if stopDetailsVM.tripData?.tripFilter == tripFilter {
                stopDetailsVM.clearTripDetails()
            }
            clearMapVehicle()
        }
        .onChange(of: tripFilter) { nextTripFilter in stopDetailsVM.handleTripFilterChange(nextTripFilter) }
        .onChange(of: stopDetailsVM.tripData?.vehicle) { vehicle in mapVM.selectedVehicle = vehicle }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .withScenePhaseHandlers(
            onActive: {
                stopDetailsVM.returnFromBackground()
                if let tripFilter {
                    stopDetailsVM.joinTripChannels(tripFilter: tripFilter)
                }
            },
            onInactive: stopDetailsVM.leaveTripChannels,
            onBackground: stopDetailsVM.leaveTripChannels
        )
    }

    @ViewBuilder private func tripDetails(
        _ tripId: String,
        _ stops: TripDetailsStopList,
        _ vehicle: Vehicle?,
        _ vehicleStop: Stop?,
        _ routeAccents: TripRouteAccents
    ) -> some View {
        let vehicleShown = vehicle != nil && vehicleStop != nil
        VStack(spacing: 0) {
            vehicleCardView(vehicle, vehicleStop, tripId, routeAccents).zIndex(1)
            TripStops(
                targetId: stopId,
                stops: stops,
                stopSequence: tripFilter?.stopSequence?.intValue,
                vehicleShown: vehicleShown,
                now: now,
                onTapLink: onTapStop,
                routeAccents: routeAccents,
                global: stopDetailsVM.global
            )
            .padding(.top, vehicleShown ? -56 : 0)
        }
    }

    @ViewBuilder
    func vehicleCardView(
        _ vehicle: Vehicle?,
        _ vehicleStop: Stop?,
        _ tripId: String,
        _ routeAccents: TripRouteAccents
    ) -> some View {
        if let vehicle, let vehicleStop {
            TripVehicleCard(
                vehicle: vehicle,
                stop: vehicleStop,
                tripId: tripId,
                routeAccents: routeAccents
            )
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderInfo = LoadingPlaceholders.shared.tripDetailsInfo()
        tripDetails(
            "",
            placeholderInfo.stops,
            placeholderInfo.vehicle,
            placeholderInfo.vehicleStop,
            TripRouteAccents()
        ).loadingPlaceholder()
    }

    private func clearMapVehicle() {
        if mapVM.selectedVehicle?.id == tripFilter?.vehicleId {
            mapVM.selectedVehicle = nil
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
                stop.resolveParent(stops: stopDetailsVM.global?.stops ?? [:]),
                filter
            )
        default: entry
        }
        nearbyVM.pushNavEntry(realEntry)
        analytics.tappedDownstreamStop(
            routeId: stopDetailsVM.tripData?.trip.routeId ?? "",
            stopId: stop.stop.id,
            tripId: tripFilter?.tripId ?? "",
            connectingRouteId: connectingRouteId
        )
    }
}
