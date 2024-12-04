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
    var statuses: [TileData]
    var alerts: [shared.Alert]
    var patternsByStop: PatternsByStop
    var pinned: Bool

    var now: Date

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel

    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared

    var showTileHeadsigns: Bool {
        patternsByStop.line != nil || !tiles.allSatisfy { tile in
            tile.headsign == tiles.first?.headsign
        }
    }

    var body: some View {
        let routeHex: String? = patternsByStop.line?.color ?? patternsByStop.representativeRoute.color
        let routeColor: Color? = if let routeHex { Color(hex: routeHex) } else { nil }
        ZStack(alignment: .top) {
            if let routeColor {
                routeColor.ignoresSafeArea(.all)
            }
            Rectangle()
                .fill(Color.halo)
                .frame(height: 2)
                .frame(maxWidth: .infinity)
            // This unscrollable scroll view is necessary to prevent the sheet from messing up the layout
            // of the contents and cutting off the header when not in large detent.
            ScrollView([], showsIndicators: false) {
                VStack(spacing: 16) {
                    ScrollViewReader { view in
                        DirectionPicker(
                            patternsByStop: patternsByStop,
                            filter: stopFilter,
                            setFilter: { stopFilter in
                                setStopFilter(stopFilter)
                                view.scrollTo(0)
                            }
                        )
                        .fixedSize(horizontal: false, vertical: true)
                        .padding([.horizontal, .top], 16)
                        .padding(.bottom, 6)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility1)

                        departureTiles(patternsByStop, view)
                            .dynamicTypeSize(...DynamicTypeSize.accessibility3)
                    }
                    alertCards(patternsByStop, routeColor)
                    statusRows(patternsByStop)

                    if let tripFilter {
                        TripDetailsView(
                            tripId: tripFilter.tripId,
                            vehicleId: tripFilter.vehicleId,
                            routeId: stopFilter.routeId,
                            stopId: stopId,
                            stopSequence: tripFilter.stopSequence?.intValue,
                            now: now,
                            global: stopDetailsVM.global,
                            errorBannerVM: errorBannerVM,
                            nearbyVM: nearbyVM,
                            mapVM: mapVM,
                            stopDetailsVM: stopDetailsVM
                        )
                    }
                }
            }
        }
        .ignoresSafeArea(.all)
    }

    @ViewBuilder
    func departureTiles(_ patternsByStop: PatternsByStop, _ view: ScrollViewProxy) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(alignment: .top, spacing: 0) {
                ForEach(Array(tiles.enumerated()), id: \.offset) { data in
                    let tileData: TileData = data.element
                    DepartureTile(
                        data: tileData,
                        onTap: {
                            if let entry = tileData.navigationTarget {
                                nearbyVM.pushNavEntry(entry)
                                withAnimation(.easeInOut(duration: 0.5)) {
                                    view.scrollTo(data.offset)
                                }
                            }
                            analytics.tappedDepartureRow(
                                routeId: patternsByStop.routeIdentifier,
                                stopId: patternsByStop.stop.id,
                                pinned: pinned,
                                alert: alerts.count > 0
                            )
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
    func alertCards(_ patternsByStop: PatternsByStop, _ routeColor: Color?) -> some View {
        ForEach(Array(alerts.enumerated()), id: \.offset) { index, alert in
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
                if index < alerts.count - 1 || !statuses.isEmpty {
                    Divider().background(Color.halo)
                }
            }
        }
    }

    @ViewBuilder
    func statusRows(_ patternsByStop: PatternsByStop) -> some View {
        ForEach(Array(statuses.enumerated()), id: \.offset) { index, row in
            VStack(spacing: 0) {
                OptionalNavigationLink(
                    value: row.navigationTarget,
                    action: { entry in
                        nearbyVM.pushNavEntry(entry)
                        analytics.tappedDepartureRow(
                            routeId: patternsByStop.routeIdentifier,
                            stopId: patternsByStop.stop.id,
                            pinned: pinned,
                            alert: alerts.count > 0
                        )
                    },
                    label: {
                        HeadsignRowView(
                            headsign: row.headsign,
                            predictions: row.formatted,
                            pillDecoration: patternsByStop.line != nil ?
                                .onRow(route: row.route) : .none
                        )
                    }
                )
                .accessibilityInputLabels([row.headsign])
                .padding(.vertical, 10)
                .padding(.horizontal, 16)

                if index < statuses.count - 1 {
                    Divider().background(Color.halo)
                }
            }
        }
    }
}
