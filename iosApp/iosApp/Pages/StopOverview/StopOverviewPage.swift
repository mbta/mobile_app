//
//  StopOverviewPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftPhoenixClient
import SwiftUI

struct StopOverviewPage: View {
    @ObserveInjection var inject
    var filters: StopOverviewPageFilters
    var navCallbacks: NavigationCallbacks

    var alerts: AlertsStreamDataResponse?
    @State var favorites: Favorites? = LoadedFavorites.last
    @State var global: GlobalResponse?
    @State var now = Date.now
    // Used only for VO notifications, VM state data is used for displaying other stop overview content
    @State var routeCardDataState: RouteCardDataViewModel.State?
    @State var vmState: StopCentricViewModel.State?

    var errorBannerVM: IErrorBannerViewModel
    var routeCardDataVM: IRouteCardDataViewModel
    var stopCentricVM: IStopCentricViewModel
    @ObservedObject var navManager: NavigationManager
    @ObservedObject var viewportProvider: ViewportProvider

    @EnvironmentObject var settingsCache: SettingsCache

    let inspection = Inspection<Self>()

    var stopId: String { filters.stopId }

    init(
        filters: StopOverviewPageFilters,
        navCallbacks: NavigationCallbacks,
        alerts: AlertsStreamDataResponse?,
        errorBannerVM: IErrorBannerViewModel,
        routeCardDataVM: IRouteCardDataViewModel,
        stopCentricVM: IStopCentricViewModel,
        navManager: NavigationManager,
        viewportProvider: ViewportProvider,
    ) {
        self.filters = filters
        self.navCallbacks = navCallbacks
        self.alerts = alerts
        self.errorBannerVM = errorBannerVM
        self.routeCardDataVM = routeCardDataVM
        self.stopCentricVM = stopCentricVM
        self.navManager = navManager
        self.viewportProvider = viewportProvider
    }

    @ViewBuilder
    var stopOverview: some View {
        StopOverviewView(
            filters: filters,
            routeData: vmState?.routeData,
            favorites: favorites,
            global: global,
            now: now.toEasternInstant(),
            setRouteStopFilter: { filter in navManager.setLastRouteStopFilter(stopId, filter) },
            navCallbacks: navCallbacks,
            errorBannerVM: errorBannerVM,
            navManager: navManager,
        )
    }

    var body: some View {
        stopOverview
            .favorites($favorites)
            .global(
                $global,
                errorKey: .companion.fromSheetTypes(sheetTypes: [.stopOverview], id: "StopOverviewPage")
            )
            .manageVM(stopCentricVM, $vmState, alerts: alerts, filters: filters, now: now.toEasternInstant())
            .manageVM(routeCardDataVM, $routeCardDataState)
            .task {
                for await filterUpdate in stopCentricVM.filterUpdates {
                    if let filterUpdate {
                        navManager.setLastRouteStopPageFilter(filterUpdate)
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
            .enableInjection()
    }

    func announceDeparture(_ previousFilters: RouteStopPageFilters) {
        guard let context = RouteStopUtils.shared.getScreenReaderTripDepartureContext(
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
