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
    var stopId: String
    var stopFilter: StopDetailsFilter?
    var tripFilter: TripDetailsFilter?

    // StopDetailsPage maintains its own internal state of the departures presented.
    // This way, when transitioning between one StopDetailsPage and another, each separate page shows
    // their respective  departures rather than both showing the departures for the newly presented stop.
    @State var internalDepartures: StopDetailsDepartures?
    @State var now = Date.now
    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @ObservedObject var mapVM: MapViewModel
    @ObservedObject var viewportProvider: ViewportProvider

    let pinnedRouteRepository = RepositoryDI().pinnedRoutes
    let globalRepository: IGlobalRepository
    let predictionsRepository: IPredictionsRepository
    let schedulesRepository: ISchedulesRepository
    let togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase
    let tripPredictionsRepository: ITripPredictionsRepository
    let tripRepository: ITripRepository
    let vehicleRepository: IVehicleRepository

    @State var globalResponse: GlobalResponse?
    @State var pinnedRoutes: Set<String> = []
    @State var predictionsByStop: PredictionsByStopJoinResponse?
    @State var schedulesResponse: ScheduleResponse?

    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared
    let inspection = Inspection<Self>()

    init(
        stopId: String,
        stopFilter: StopDetailsFilter?,
        tripFilter: TripDetailsFilter?,
        internalDepartures _: StopDetailsDepartures? = nil,

        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel,
        mapVM: MapViewModel,
        viewportProvider: ViewportProvider,

        globalRepository: IGlobalRepository = RepositoryDI().global,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        tripPredictionsRepository: ITripPredictionsRepository = RepositoryDI().tripPredictions,
        tripRepository: ITripRepository = RepositoryDI().trip,
        vehicleRepository: IVehicleRepository = RepositoryDI().vehicle
    ) {
        self.stopId = stopId
        self.stopFilter = stopFilter
        self.tripFilter = tripFilter

        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
        self.mapVM = mapVM
        self.viewportProvider = viewportProvider

        self.globalRepository = globalRepository
        self.schedulesRepository = schedulesRepository
        self.predictionsRepository = predictionsRepository
        self.tripPredictionsRepository = tripPredictionsRepository
        self.tripRepository = tripRepository
        self.vehicleRepository = vehicleRepository
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
            global: globalResponse,
            pinnedRoutes: pinnedRoutes,
            togglePinnedRoute: togglePinnedRoute,
            now: now,
            errorBannerVM: errorBannerVM,
            nearbyVM: nearbyVM,
            mapVM: mapVM,
            tripPredictionsRepository: tripPredictionsRepository,
            tripRepository: tripRepository,
            vehicleRepository: vehicleRepository
        )
    }

    var body: some View {
        stopDetails
            .onChange(of: stopId) { nextStopId in changeStop(nextStopId) }
            .onChange(of: globalResponse) { nextGlobal in updateDepartures(globalResponse: nextGlobal) }
            .onChange(of: pinnedRoutes) { nextPinned in updateDepartures(pinnedRoutes: nextPinned) }
            .onChange(of: predictionsByStop) { nextPredictions in updateDepartures(predictionsByStop: nextPredictions) }
            .onChange(of: schedulesResponse) { nextSchedules in updateDepartures(schedulesResponse: nextSchedules) }
            .onChange(of: stopFilter) { nextStopFilter in setTripFilter(stopFilter: nextStopFilter) }
            .onChange(of: internalDepartures) { nextDepartures in setTripFilter(departures: nextDepartures) }
            .onAppear { loadEverything() }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .task(id: stopId) {
                while !Task.isCancelled {
                    now = Date.now
                    updateDepartures()
                    checkPredictionsStale()
                    try? await Task.sleep(for: .seconds(5))
                }
            }
            .onDisappear { leavePredictions() }
            .withScenePhaseHandlers(
                onActive: {
                    if let predictionsByStop,
                       predictionsRepository
                       .shouldForgetPredictions(predictionCount: predictionsByStop.predictionQuantity()) {
                        self.predictionsByStop = nil
                    }
                    joinPredictions(stopId)
                },
                onInactive: leavePredictions,
                onBackground: {
                    leavePredictions()
                    errorBannerVM.loadingWhenPredictionsStale = true
                }
            )
    }
}
