//
//  StopDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftPhoenixClient
import SwiftUI

struct RouteCardParams: Equatable {
    let alerts: AlertsStreamDataResponse?
    let global: GlobalResponse?
    let now: Date
    let stopData: StopData?
    let stopFilter: StopDetailsFilter?
    let stopId: String
}

struct StopDetailsPage: View {
    var filters: StopDetailsPageFilters

    // StopDetailsPage maintains its own internal state of the departures presented.
    // This way, when transitioning between one StopDetailsPage and another, each separate page shows
    // their respective departures rather than both showing the departures for the newly presented stop.

    @State var internalRouteCardData: [RouteCardData]?
    @State var now = Date.now

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: iosApp.MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    @EnvironmentObject var settingsCache: SettingsCache

    let inspection = Inspection<Self>()

    var stopId: String { filters.stopId }
    var stopFilter: StopDetailsFilter? { filters.stopFilter }
    var tripFilter: TripDetailsFilter? { filters.tripFilter }

    init(
        filters: StopDetailsPageFilters,
        errorBannerVM: IErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: iosApp.MapViewModel,
        stopDetailsVM: StopDetailsViewModel,
        viewportProvider: ViewportProvider
    ) {
        self.filters = filters

        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.stopDetailsVM = stopDetailsVM
        self.viewportProvider = viewportProvider
    }

    @ViewBuilder
    var stopDetails: some View {
        StopDetailsView(
            stopId: stopId,
            stopFilter: stopFilter,
            tripFilter: tripFilter,
            setStopFilter: { filter in nearbyVM.setLastStopDetailsFilter(stopId, filter) },
            setTripFilter: { filter in nearbyVM.setLastTripDetailsFilter(stopId, filter) },
            routeCardData: internalRouteCardData,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM
        )
    }

    var body: some View {
        stopDetails
            .onChange(of: stopFilter) { newStopFilter in
                if newStopFilter == nil {
                    internalRouteCardData = nil
                }
            }
            .onChange(of: stopDetailsVM.stopData) { stopData in
                errorBannerVM.setIsLoadingWhenPredictionsStale(isLoading: !(stopData?.predictionsLoaded ?? true))
            }
            .onChange(of: filters) { nextFilters in setTripFilter(filters: nextFilters) }
            .onChange(of: RouteCardParams(
                alerts: nearbyVM.alerts,
                global: stopDetailsVM.global,
                now: now,
                stopData: stopDetailsVM.stopData,
                stopFilter: stopFilter,
                stopId: stopId
            )) { newParams in
                updateDepartures(routeCardParams: newParams)
            }
            .onChange(of: internalRouteCardData) { newInternalRouteCardData in
                let nextStopFilter = setStopFilter(newInternalRouteCardData)
                setTripFilter(filters: .init(stopId: stopId, stopFilter: nextStopFilter, tripFilter: tripFilter))
            }
            .task(id: stopId) {
                while !Task.isCancelled {
                    now = Date.now
                    stopDetailsVM.checkStopPredictionsStale()
                    try? await Task.sleep(for: .seconds(5))
                }
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .withScenePhaseHandlers(
                onActive: {
                    stopDetailsVM.returnFromBackground()
                    stopDetailsVM.joinStopPredictions(stopId)
                },
                onInactive: stopDetailsVM.leaveStopPredictions,
                onBackground: {
                    stopDetailsVM.leaveStopPredictions()
                    errorBannerVM.setIsLoadingWhenPredictionsStale(isLoading: true)
                }
            )
    }

    func announceDeparture(_ previousFilters: StopDetailsPageFilters) {
        guard let context = StopDetailsUtils.shared.getScreenReaderTripDepartureContext(
            routeCardData: internalRouteCardData,
            previousFilters: previousFilters
        ) else { return }
        let routeType = context.routeType.typeText(isOnly: true)
        let stopName = context.stopName

        let announcementString = if let destination = context.destination {
            String(format: NSLocalizedString(
                "%1$@ to %2$@ has departed %3$@",
                comment: """
                Screen reader text that is announced when a trip disappears from the screen.,
                in the format "[train/bus/ferry] to [destination] has departed [stop name]",
                ex. "[train] to [Alewife] has departed [Central]", "[bus] to [Nubian] has departed [Harvard]"
                """
            ), routeType, destination, stopName)
        } else {
            String(format: NSLocalizedString(
                "%1$@ has departed %2$@",
                comment: """
                Screen reader text that is announced when a trip disappears from the screen.,
                in the format "[train/bus/ferry] to [destination] has departed [stop name]",
                ex. "[train] has departed [Central]", "[bus] has departed [Harvard]"
                """
            ), routeType, stopName)
        }

        if #available(iOS 17, *) {
            var departureAnnouncement = AttributedString(announcementString)
            departureAnnouncement.accessibilitySpeechAnnouncementPriority = .high
            AccessibilityNotification.Announcement(departureAnnouncement).post()
        } else {
            UIAccessibility.post(
                notification: .layoutChanged,
                argument: announcementString
            )
        }
    }

    func setStopFilter(_ routeCardData: [RouteCardData]?) -> StopDetailsFilter? {
        let nextStopFilter = stopFilter ?? StopDetailsUtils.shared.autoStopFilter(routeCardData: routeCardData)
        if stopFilter != nextStopFilter {
            nearbyVM.setLastStopDetailsFilter(stopId, nextStopFilter)
        }
        return nextStopFilter
    }

    func setTripFilter(filters: StopDetailsPageFilters) {
        let tripFilter = StopDetailsUtils.shared.autoTripFilter(
            routeCardData: internalRouteCardData,
            stopFilter: filters.stopFilter,
            currentTripFilter: filters.tripFilter,
            filterAtTime: now.toEasternInstant(),
            globalData: stopDetailsVM.global
        )

        if let previousFilter = filters.tripFilter, tripFilter != previousFilter {
            announceDeparture(filters)
        }

        nearbyVM.setLastTripDetailsFilter(stopId, tripFilter)
    }

    func updateDepartures(routeCardParams: RouteCardParams) {
        Task {
            if routeCardParams.stopId != routeCardParams.stopData?.stopId {
                return
            }
            let nextRouteCardData = await stopDetailsVM.getRouteCardData(
                stopId: routeCardParams.stopId,
                alerts: routeCardParams.alerts,
                now: routeCardParams.now.toEasternInstant(),
                isFiltered: routeCardParams.stopFilter != nil
            )
            Task { @MainActor in
                nearbyVM.setRouteCardData(routeCardParams.stopId, nextRouteCardData)
                internalRouteCardData = nextRouteCardData
            }
        }
    }

    // Testing convenience
    func updateDepartures() {
        updateDepartures(routeCardParams: RouteCardParams(alerts: nearbyVM.alerts,
                                                          global: stopDetailsVM.global,
                                                          now: now,
                                                          stopData: stopDetailsVM.stopData,
                                                          stopFilter: stopFilter,
                                                          stopId: stopId))
    }
}
