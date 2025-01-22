//
//  StopDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftPhoenixClient
import SwiftUI

struct StopDetailsPage: View {
    var filters: StopDetailsPageFilters

    // StopDetailsPage maintains its own internal state of the departures presented.
    // This way, when transitioning between one StopDetailsPage and another, each separate page shows
    // their respective  departures rather than both showing the departures for the newly presented stop.
    @State var internalDepartures: StopDetailsDepartures?
    @State var now = Date.now

    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var stopDetailsVM: StopDetailsViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    let inspection = Inspection<Self>()

    var stopId: String { filters.stopId }
    var stopFilter: StopDetailsFilter? { filters.stopFilter }
    var tripFilter: TripDetailsFilter? { filters.tripFilter }

    init(
        filters: StopDetailsPageFilters,
        internalDepartures _: StopDetailsDepartures? = nil,

        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
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
            departures: internalDepartures,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM
        )
    }

    var body: some View {
        stopDetails
            .fullScreenCover(
                isPresented: .init(
                    get: { stopDetailsVM.explainer != nil },
                    set: { value in if !value { stopDetailsVM.explainer = nil } }
                )
            ) {
                if let explainer = stopDetailsVM.explainer {
                    ExplainerPage(
                        explainer: explainer,
                        onClose: { stopDetailsVM.explainer = nil }
                    )
                }
            }
            .onChange(of: stopDetailsVM.global) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.pinnedRoutes) { _ in updateDepartures() }
            .onChange(of: stopDetailsVM.stopData) { stopData in
                errorBannerVM.loadingWhenPredictionsStale = !(stopData?.predictionsLoaded ?? true)
                updateDepartures()
            }
            .onChange(of: filters) { nextFilters in setTripFilter(filters: nextFilters) }
            .onChange(of: internalDepartures) { _ in
                let nextStopFilter = setStopFilter()
                setTripFilter(filters: .init(stopId: stopId, stopFilter: nextStopFilter, tripFilter: tripFilter))
            }
            .task(id: stopId) {
                while !Task.isCancelled {
                    now = Date.now
                    updateDepartures()
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
                    errorBannerVM.loadingWhenPredictionsStale = true
                }
            )
    }

    func announceDeparture(_ previousFilters: StopDetailsPageFilters) {
        guard let context = internalDepartures?.getScreenReaderTripDepartureContext(
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

    func setStopFilter() -> StopDetailsFilter? {
        let nextStopFilter = stopFilter ?? internalDepartures?.autoStopFilter()
        if stopFilter != nextStopFilter {
            nearbyVM.setLastStopDetailsFilter(stopId, nextStopFilter)
        }
        return nextStopFilter
    }

    func setTripFilter(filters: StopDetailsPageFilters) {
        let tripFilter = internalDepartures?.autoTripFilter(
            stopFilter: filters.stopFilter,
            currentTripFilter: filters.tripFilter,
            filterAtTime: now.toKotlinInstant()
        )

        if let previousFilter = filters.tripFilter, tripFilter != previousFilter {
            announceDeparture(filters)
        }

        nearbyVM.setLastTripDetailsFilter(stopId, tripFilter)
    }

    func updateDepartures() {
        Task {
            if stopId != stopDetailsVM.stopData?.stopId { return }
            let nextDepartures = stopDetailsVM.getDepartures(
                stopId: stopId,
                alerts: nearbyVM.alerts,
                useTripHeadsigns: nearbyVM.tripHeadsignsEnabled,
                now: now
            )
            Task { @MainActor in
                nearbyVM.setDepartures(stopId, nextDepartures)
                internalDepartures = nextDepartures
            }
        }
    }
}
