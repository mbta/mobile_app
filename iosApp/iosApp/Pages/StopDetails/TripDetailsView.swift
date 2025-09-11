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
    var tripFilter: TripDetailsPageFilter?

    var now: EasternTimeInstant
    var alertSummaries: [String: AlertSummary?]

    let onOpenAlertDetails: (Shared.Alert) -> Void

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    var mapVM: IMapViewModel
    var tripDetailsVM: ITripDetailsViewModel

    @State var explainer: Explainer?
    @State var global: GlobalResponse?
    @State var tripDetailsVMState: TripDetailsViewModel.State?

    @EnvironmentObject var settingsCache: SettingsCache

    let analytics: Analytics
    var didLoadData: ((Self) -> Void)?
    let inspection = Inspection<Self>()

    init(
        tripFilter: TripDetailsPageFilter?,
        now: EasternTimeInstant,
        alertSummaries: [String: AlertSummary?],
        onOpenAlertDetails: @escaping (Shared.Alert) -> Void,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: IMapViewModel,
        tripDetailsVM: ITripDetailsViewModel = ViewModelDI().tripDetails,
        analytics: Analytics = AnalyticsProvider.shared
    ) {
        self.tripFilter = tripFilter
        self.now = now
        self.alertSummaries = alertSummaries
        self.onOpenAlertDetails = onOpenAlertDetails
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.tripDetailsVM = tripDetailsVM
        self.analytics = analytics
    }

    func getParentFor(_ stopId: String?) -> Stop? {
        if let global {
            global.getStop(stopId: stopId)?.resolveParent(global: global)
        } else { nil }
    }

    var body: some View {
        content
            .explainer($explainer)
            .global($global, errorKey: "TripDetailsView")
            .manageVM(
                tripDetailsVM,
                $tripDetailsVMState,
                alerts: nearbyVM.alerts,
                context: .stopDetails,
                filters: tripFilter,
            )
            .task(id: global) {
                for await vehicleUpdate in tripDetailsVM.selectedVehicleUpdates {
                    setVehicle(vehicleUpdate)
                }
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
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
            let vehicle = tripDetailsVMState?.tripData?.vehicle
            if let tripFilter,
               tripFilter.vehicleId != nil ? vehicle != nil : true,
               let stops = tripDetailsVMState?.stopList,
               let tripData = tripDetailsVMState?.tripData,
               tripData.tripFilter == tripFilter,
               tripData.tripPredictionsLoaded,
               let global,
               let route = global.getRoute(routeId: tripData.trip.routeId) {
                let terminalStop = getParentFor(tripData.trip.stopIds?.first)
                let vehicleStop = getParentFor(vehicle?.stopId)
                tripDetails(tripFilter, tripData.trip, stops, terminalStop, vehicle, vehicleStop, route)
                    .onAppear { didLoadData?(self) }
            } else {
                loadingBody()
            }
        }
    }

    @ViewBuilder private func tripDetails(
        _ filter: TripDetailsPageFilter,
        _ trip: Trip,
        _ stops: TripDetailsStopList,
        _ terminalStop: Stop?,
        _ vehicle: Vehicle?,
        _ vehicleStop: Stop?,
        _ route: Route,
    ) -> some View {
        let routeAccents = TripRouteAccents(route: route)
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
            explainer = .init(type: explainerType, routeAccents: routeAccents)
        } } else { nil }

        VStack(spacing: 0) {
            tripHeaderCard(filter.stopId, trip, headerSpec, onHeaderTap, route, routeAccents).zIndex(1)
                .padding(.horizontal, 6)
            TripStops(
                targetId: filter.stopId,
                stops: stops,
                stopSequence: filter.stopSequence?.intValue,
                headerSpec: headerSpec,
                now: now,
                alertSummaries: alertSummaries,
                onTapLink: onTapStop,
                onOpenAlertDetails: onOpenAlertDetails,
                route: route,
                routeAccents: routeAccents,
                global: global
            )
            .padding(.top, -56)
        }
    }

    @ViewBuilder
    func tripHeaderCard(
        _ stopId: String,
        _ trip: Trip,
        _ spec: TripHeaderSpec?,
        _ onTap: (() -> Void)?,
        _ route: Route,
        _ routeAccents: TripRouteAccents
    ) -> some View {
        if let spec {
            TripHeaderCard(
                spec: spec,
                trip: trip,
                targetId: stopId,
                route: route,
                routeAccents: routeAccents,
                onTap: onTap,
                now: now
            )
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        let placeholderInfo = LoadingPlaceholders.shared.tripDetailsInfo()
        tripDetails(
            .init(
                stopId: "",
                stopFilter: .init(routeId: "", directionId: 0),
                tripFilter: .init(tripId: "", vehicleId: nil, stopSequence: nil, selectionLock: false)
            ),
            placeholderInfo.trip,
            placeholderInfo.stops,
            nil,
            placeholderInfo.vehicle,
            placeholderInfo.vehicleStop,
            placeholderInfo.route,
        ).loadingPlaceholder()
    }

    private func setVehicle(_ vehicle: Vehicle?) {
        let stop = global?.getStop(stopId: nearbyVM.navigationStack.lastStopId)
        let stopFilter = nearbyVM.navigationStack.lastStopDetailsFilter
        let tripDetailsFilter = nearbyVM.navigationStack.lastTripDetailsFilter
        if let tripDetailsFilter {
            mapVM.selectedTrip(
                stopFilter: stopFilter,
                stop: stop,
                tripFilter: tripDetailsFilter,
                vehicle: vehicle
            )
        }
    }

    func onTapStop(stop: TripDetailsStopList.Entry) {
        let parentStop = if let global { stop.stop.resolveParent(global: global) }
        else { stop.stop }
        nearbyVM.appendNavEntry(.stopDetails(stopId: parentStop.id, stopFilter: nil, tripFilter: nil))
        analytics.tappedDownstreamStop(
            routeId: tripDetailsVMState?.tripData?.trip.routeId ?? "",
            stopId: stop.stop.id,
            tripId: tripFilter?.tripId ?? "",
            connectingRouteId: nil
        )
    }
}
