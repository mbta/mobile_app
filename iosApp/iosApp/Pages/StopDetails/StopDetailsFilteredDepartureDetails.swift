//
//  StopDetailsFilteredDepartureDetails.swift
//  iosApp
//
//  Created by esimon on 12/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct StopDetailsFilteredDepartureDetails: View {
    var stopId: String
    var stopFilter: StopDetailsFilter
    var tripFilter: TripDetailsFilter?
    var setStopFilter: (StopDetailsFilter?) -> Void
    var setTripFilter: (TripDetailsFilter?) -> Void

    var tiles: [TileData]
    var noPredictionsStatus: RealtimePatterns.NoTripsFormat?
    var alerts: [shared.Alert]
    var patternsByStop: PatternsByStop
    var pinned: Bool

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    @EnvironmentObject var viewportProvider: ViewportProvider

    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared

    var showTileHeadsigns: Bool {
        patternsByStop.line != nil || !tiles.allSatisfy { tile in
            tile.headsign == tiles.first?.headsign
        }
    }

    var stop: Stop? { stopDetailsVM.global?.stops[stopId] }

    var routeColor: Color { Color(hex: patternsByStop.representativeRoute.color) }
    var routeType: RouteType { patternsByStop.representativeRoute.type }
    var headerColor: Color {
        // Regular bus color needs to be overridden for contrast, but SL should not be
        let isSL = MapStopRoute.silver.matches(route: patternsByStop.representativeRoute)
        return if routeType == .bus, !isSL { Color.text } else { routeColor }
    }

    var selectedTripIsCancelled: Bool {
        if let tripFilter {
            patternsByStop.patterns.contains { pattern in
                pattern.upcomingTrips.contains { trip in
                    trip.trip.id == tripFilter.tripId && trip.isCancelled
                }
            }
        } else {
            false
        }
    }

    var body: some View {
        ZStack(alignment: .top) {
            routeColor.ignoresSafeArea(.all)
            Rectangle()
                .fill(Color.halo)
                .frame(height: 2)
                .frame(maxWidth: .infinity)
            ScrollView(.vertical, showsIndicators: false) {
                VStack(spacing: 16) {
                    ScrollViewReader { view in
                        DirectionPicker(
                            patternsByStop: patternsByStop,
                            filter: stopFilter,
                            setFilter: { setStopFilter($0) }
                        )
                        .onChange(of: tripFilter) { filter in if let filter { view.scrollTo(filter.tripId) } }
                        .fixedSize(horizontal: false, vertical: true)
                        .padding([.horizontal, .top], 16)
                        .padding(.bottom, 6)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility1)

                        if !tiles.isEmpty {
                            departureTiles(view)
                                .dynamicTypeSize(...DynamicTypeSize.accessibility3)
                                .onAppear { if let id = tripFilter?.tripId { view.scrollTo(id) } }
                        }
                    }
                    alertCards

                    if let noPredictionsStatus {
                        StopDetailsNoTripCard(
                            status: noPredictionsStatus,
                            headerColor: headerColor,
                            routeType: routeType
                        )
                    } else if selectedTripIsCancelled {
                        StopDetailsIconCard(
                            details: Text(
                                "This trip has been cancelled. We’re sorry for the inconvenience.",
                                comment: "Explanation for a cancelled trip on stop details"
                            ),
                            header: Text(
                                "Trip cancelled",
                                comment: "Header for a cancelled trip card on stop details"
                            ),
                            headerColor: headerColor,
                            icon: routeSlashIcon(routeType)
                        )
                    } else {
                        TripDetailsView(
                            tripFilter: tripFilter,
                            stopId: stopId,
                            now: now,
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM,
                            mapVM: mapVM,
                            stopDetailsVM: stopDetailsVM
                        )
                    }
                }
            }
        }
        .onAppear { handleViewportForStatus(noPredictionsStatus) }
        .onChange(of: noPredictionsStatus) { status in handleViewportForStatus(status) }
        .onChange(of: selectedTripIsCancelled) { if $0 { setViewportToStop() } }
        .ignoresSafeArea(.all)
    }

    func handleViewportForStatus(_ status: RealtimePatterns.NoTripsFormat?) {
        if let status {
            switch onEnum(of: status) {
            case .predictionsUnavailable: setViewportToStop(midZoom: true)
            case .noSchedulesToday, .serviceEndedToday: setViewportToStop()
            }
        }
    }

    func setViewportToStop(midZoom: Bool = false) {
        if let stop {
            viewportProvider.animateTo(
                coordinates: stop.coordinate,
                zoom: midZoom ? MapDefaults.shared.midZoomThreshold : nil
            )
        }
    }

    @ViewBuilder
    func departureTiles(_ view: ScrollViewProxy) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .top, spacing: 0) {
                ForEach(tiles) { tileData in
                    DepartureTile(
                        data: tileData,
                        onTap: {
                            if let upcoming = tileData.upcoming {
                                nearbyVM.navigationStack.lastTripDetailsFilter = .init(
                                    tripId: upcoming.trip.id,
                                    vehicleId: upcoming.prediction?.vehicleId,
                                    stopSequence: upcoming.stopSequence,
                                    selectionLock: false
                                )
                                analytics.tappedDepartureRow(
                                    routeId: patternsByStop.routeIdentifier,
                                    stopId: patternsByStop.stop.id,
                                    pinned: pinned,
                                    alert: alerts.count > 0
                                )
                                view.scrollTo(tileData.id)
                            }
                        },
                        pillDecoration: patternsByStop
                            .line != nil ? .onPrediction(route: tileData.route) : .none,
                        showHeadsign: showTileHeadsigns,
                        isSelected: tileData.upcoming?.trip.id == tripFilter?.tripId
                    ).padding(.horizontal, 4)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 1)
            .fixedSize(horizontal: false, vertical: true)
        }
    }

    @ViewBuilder
    var alertCards: some View {
        ForEach(alerts, id: \.id) { alert in
            VStack(spacing: 0) {
                StopDetailsAlertHeader(alert: alert, routeColor: routeColor)
                    .onTapGesture {
                        nearbyVM.pushNavEntry(.alertDetails(
                            alertId: alert.id,
                            line: patternsByStop.line,
                            routes: patternsByStop.routes
                        ))
                        analytics.tappedAlertDetails(
                            routeId: patternsByStop.routeIdentifier,
                            stopId: patternsByStop.stop.id,
                            alertId: alert.id
                        )
                    }.padding(.horizontal, 8)
            }
        }
    }
}
