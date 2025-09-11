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

    @State var favorites: Favorites = LoadedFavorites.last
    @State var global: GlobalResponse?
    @State var loadingFavorites = true
    @State var now = Date.now
    // Used only for VO notifications, VM state data is used for displaying other stop details content
    @State var routeCardDataState: RouteCardDataViewModel.State?
    @State var vmState: StopDetailsViewModel.State?

    var errorBannerVM: IErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    var mapVM: IMapViewModel
    var routeCardDataVM: IRouteCardDataViewModel
    var stopDetailsVM: IStopDetailsViewModel
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
        mapVM: IMapViewModel,
        routeCardDataVM: IRouteCardDataViewModel,
        stopDetailsVM: IStopDetailsViewModel,
        viewportProvider: ViewportProvider
    ) {
        self.filters = filters
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.routeCardDataVM = routeCardDataVM
        self.stopDetailsVM = stopDetailsVM
        self.viewportProvider = viewportProvider
    }

    @ViewBuilder
    var stopDetails: some View {
        StopDetailsView(
            filters: filters,
            routeData: vmState?.routeData,
            favorites: favorites,
            global: global,
            now: now,
            onUpdateFavorites: { loadingFavorites = true },
            setStopFilter: { filter in nearbyVM.setLastStopDetailsFilter(stopId, filter) },
            setTripFilter: { filter in nearbyVM.setLastTripDetailsFilter(stopId, filter) },
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            stopDetailsVM: stopDetailsVM
        )
    }

    var body: some View {
        stopDetails
            .favorites($favorites, awaitingUpdate: $loadingFavorites)
            .global($global, errorKey: "StopDetailsPage")
            .manageVM(stopDetailsVM, $vmState, alerts: nearbyVM.alerts, filters: filters, now: now.toEasternInstant())
            .manageVM(routeCardDataVM, $routeCardDataState)
            .task {
                for await filterUpdate in stopDetailsVM.filterUpdates {
                    if let filterUpdate {
                        nearbyVM.setLastStopDetailsPageFilter(filterUpdate)
                    }
                }
            }
            .onChange(of: vmState?.awaitingPredictionsAfterBackground ?? true) { isLoading in
                errorBannerVM.setIsLoadingWhenPredictionsStale(isLoading: isLoading)
            }
            .task(id: stopId) {
                while !Task.isCancelled {
                    now = Date.now
                    try? await Task.sleep(for: .seconds(5))
                }
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }

    func announceDeparture(_ previousFilters: StopDetailsPageFilters) {
        guard let context = StopDetailsUtils.shared.getScreenReaderTripDepartureContext(
            routeCardData: routeCardDataState?.data,
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
}
