//
//  TripDetailsView.swift
//  iosApp
//
//  Created by esimon on 11/14/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import Shared
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

    @State var stops: TripDetailsStopList?

    @EnvironmentObject var settingsCache: SettingsCache

    let onOpenAlertDetails: (Shared.Alert) -> Void

    let analytics: Analytics
    var didLoadData: ((Self) -> Void)?
    let inspection = Inspection<Self>()

    init(
        tripFilter: TripDetailsFilter?,
        stopId: String,
        now: Date,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        stopDetailsVM: StopDetailsViewModel,
        onOpenAlertDetails: @escaping (Shared.Alert) -> Void,
        analytics: Analytics = AnalyticsProvider.shared
    ) {
        self.tripFilter = tripFilter
        self.stopId = stopId
        self.now = now
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
        self.onOpenAlertDetails = onOpenAlertDetails

        self.analytics = analytics
    }

    func getParentFor(_ stopId: String?, global: GlobalResponse) -> Stop? {
        global.getStop(stopId: stopId)?.resolveParent(global: global)
    }

    var body: some View {
        content
            .task { stopDetailsVM.handleTripFilterChange(tripFilter) }
            .onAppear { updateStops() }
            .onDisappear {
                if stopDetailsVM.tripData?.tripFilter == tripFilter || tripFilter == nil {
                    stopDetailsVM.clearTripDetails()
                }
                clearMapVehicle()
            }
            .onChange(of: tripFilter) { nextTripFilter in stopDetailsVM.handleTripFilterChange(nextTripFilter)
                updateStops()
            }
            .onChange(of: stopDetailsVM.tripData?.vehicle) { vehicle in mapVM.selectedVehicle = vehicle
                updateStops()
            }
            .onChange(of: stopDetailsVM.tripData?.tripFilter) { _ in updateStops() }
            .onChange(of: stopDetailsVM.tripData?.tripPredictionsLoaded) { _ in updateStops() }
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

    @ViewBuilder private var content: some View {
        VStack(spacing: 16) {
            if settingsCache.get(.devDebugMode) {
                DebugView {
                    VStack {
                        Text(verbatim: "trip id: \(tripFilter?.tripId ?? "nil")")
                        Text(verbatim: "vehicle id: \(tripFilter?.vehicleId ?? "nil")")
                    }
                }
                .padding(.horizontal, 6)
            }
            let vehicle = stopDetailsVM.tripData?.vehicle
            if let tripFilter,
               tripFilter.vehicleId != nil ? vehicle != nil : true,
               let tripData = stopDetailsVM.tripData,
               tripData.tripFilter == tripFilter,
               tripData.tripPredictionsLoaded,
               let global = stopDetailsVM.global,
               let stops {
                let routeAccents = stopDetailsVM.getTripRouteAccents()
                let terminalStop = getParentFor(tripData.trip.stopIds?.first, global: global)
                let vehicleStop = getParentFor(vehicle?.stopId, global: global)
                tripDetails(tripData.trip, stops, terminalStop, vehicle, vehicleStop, routeAccents)
                    .onAppear { didLoadData?(self) }
            } else {
                loadingBody()
            }
        }
    }

    @ViewBuilder private func tripDetails(
        _ trip: Trip,
        _ stops: TripDetailsStopList,
        _ terminalStop: Stop?,
        _ vehicle: Vehicle?,
        _ vehicleStop: Stop?,
        _ routeAccents: TripRouteAccents
    ) -> some View {
        let headerSpec: TripHeaderSpec? = {
            if let vehicle, let vehicleStop {
                let atTerminal = terminalStop != nil && terminalStop?.id == vehicleStop.id
                    && vehicle.currentStatus == .stoppedAt
                let entry = atTerminal ? stops.startTerminalEntry : stops.stops.first { entry in
                    entry.stop.id == vehicleStop.id
                }
                return vehicle.tripId == trip.id
                    ? .vehicle(vehicle, vehicleStop, entry, atTerminal)
                    : .finishingAnotherTrip
            } else if stops.stops.contains(where: { entry in entry.prediction != nil }) {
                return .noVehicle
            } else if let terminalStop, let terminalEntry = stops.startTerminalEntry {
                return .scheduled(terminalStop, terminalEntry)
            } else { return nil }
        }()

        let explainerType: ExplainerType? = switch headerSpec {
        case .scheduled: routeAccents.type != .ferry ? .noPrediction : nil
        case .finishingAnotherTrip: .finishingAnotherTrip
        case .noVehicle: .noVehicle
        default: nil
        }
        let onHeaderTap: (() -> Void)? = if let explainerType { {
            stopDetailsVM.explainer = .init(type: explainerType, routeAccents: routeAccents)
        } } else { nil }

        VStack(spacing: 0) {
            tripHeaderCard(trip, headerSpec, onHeaderTap, routeAccents).zIndex(1)
                .padding(.horizontal, 6)
            TripStops(
                targetId: stopId,
                stops: stops,
                stopSequence: tripFilter?.stopSequence?.intValue,
                headerSpec: headerSpec,
                now: now,
                alertSummaries: stopDetailsVM.alertSummaries,
                onTapLink: onTapStop,
                onOpenAlertDetails: onOpenAlertDetails,
                routeAccents: routeAccents,
                global: stopDetailsVM.global
            )
            .padding(.top, -56)
        }
    }

    @ViewBuilder
    func tripHeaderCard(
        _ trip: Trip,
        _ spec: TripHeaderSpec?,
        _ onTap: (() -> Void)?,
        _ routeAccents: TripRouteAccents
    ) -> some View {
        if let spec {
            TripHeaderCard(
                spec: spec,
                trip: trip,
                targetId: stopId,
                routeAccents: routeAccents,
                onTap: onTap,
                now: now
            )
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderInfo = LoadingPlaceholders.shared.tripDetailsInfo()
        tripDetails(
            placeholderInfo.trip,
            placeholderInfo.stops,
            nil,
            placeholderInfo.vehicle,
            placeholderInfo.vehicleStop,
            TripRouteAccents()
        ).loadingPlaceholder()
    }

    private func updateStops() {
        Task {
            let vehicle = stopDetailsVM.tripData?.vehicle
            if let tripFilter,
               tripFilter.vehicleId != nil ? vehicle != nil : true,
               let tripData = stopDetailsVM.tripData,
               tripData.tripFilter == tripFilter,
               tripData.tripPredictionsLoaded,
               let global = stopDetailsVM.global {
                stops = try await TripDetailsStopList.companion.fromPieces(
                    trip: tripData.trip,
                    tripSchedules: tripData.tripSchedules,
                    tripPredictions: tripData.tripPredictions,
                    vehicle: vehicle,
                    alertsData: nearbyVM.alerts,
                    globalData: global
                )
            } else {
                stops = nil
            }
        }
    }

    private func clearMapVehicle() {
        if mapVM.selectedVehicle?.id == tripFilter?.vehicleId {
            mapVM.selectedVehicle = nil
        }
    }

    func onTapStop(stop: TripDetailsStopList.Entry) {
        let parentStop = if let global = stopDetailsVM.global { stop.stop.resolveParent(global: global) }
        else { stop.stop }
        nearbyVM.appendNavEntry(.stopDetails(stopId: parentStop.id, stopFilter: nil, tripFilter: nil))
        analytics.tappedDownstreamStop(
            routeId: stopDetailsVM.tripData?.trip.routeId ?? "",
            stopId: stop.stop.id,
            tripId: tripFilter?.tripId ?? "",
            connectingRouteId: nil
        )
    }
}
