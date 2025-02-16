//
//  LegacyStopDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftPhoenixClient
import SwiftUI

struct LegacyStopDetailsPage: View {
    var analytics: Analytics = AnalyticsProvider.shared
    let globalRepository: IGlobalRepository
    @State var globalResponse: GlobalResponse?
    @ObservedObject var viewportProvider: ViewportProvider
    let schedulesRepository: ISchedulesRepository
    @State var schedulesResponse: ScheduleResponse?
    var pinnedRouteRepository = RepositoryDI().pinnedRoutes
    var togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase

    @State var predictionsRepository: IPredictionsRepository
    var stop: Stop

    var filter: StopDetailsFilter?
    // LegacyStopDetailsPage maintains its own internal state of the departures presented.
    // This way, when transitioning between one LegacyStopDetailsPage and another, each separate page shows
    // their respective  departures rather than both showing the departures for the newly presented stop.
    @State var internalDepartures: StopDetailsDepartures?
    @State var now = Date.now
    @ObservedObject var errorBannerVM: ErrorBannerViewModel
    @ObservedObject var nearbyVM: NearbyViewModel
    @State var pinnedRoutes: Set<String> = []
    @State var predictionsByStop: PredictionsByStopJoinResponse?

    let inspection = Inspection<Self>()

    var didAppear: ((Self) -> Void)?

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: StopDetailsFilter?,
        internalDepartures: StopDetailsDepartures? = nil,
        errorBannerVM: ErrorBannerViewModel,
        nearbyVM: NearbyViewModel
    ) {
        self.globalRepository = globalRepository
        self.schedulesRepository = schedulesRepository
        self.predictionsRepository = predictionsRepository
        self.viewportProvider = viewportProvider
        self.stop = stop
        self.filter = filter
        self.internalDepartures = internalDepartures // only for testing
        self.errorBannerVM = errorBannerVM
        self.nearbyVM = nearbyVM
    }

    var body: some View {
        VStack {
            LegacyStopDetailsView(
                stop: stop,
                filter: filter,
                setFilter: { filter in nearbyVM.pushNavEntry(.legacyStopDetails(stop, filter)) },
                departures: internalDepartures,
                errorBannerVM: errorBannerVM,
                nearbyVM: nearbyVM,
                now: now,
                pinnedRoutes: pinnedRoutes,
                togglePinnedRoute: togglePinnedRoute
            )
            .onAppear {
                loadEverything()
                didAppear?(self)
            }
            .onChange(of: stop) { nextStop in
                changeStop(nextStop)
            }
            .onChange(of: globalResponse) { _ in
                updateDepartures(stop)
            }
            .onChange(of: pinnedRoutes) { _ in
                updateDepartures(stop)
            }
            .onChange(of: predictionsByStop) { newPredictionsByStop in
                updateDepartures(stop, newPredictionsByStop)
            }
            .onChange(of: schedulesResponse) { _ in
                updateDepartures(stop)
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .task(id: stop.id) {
                while !Task.isCancelled {
                    now = Date.now
                    updateDepartures()
                    checkPredictionsStale()
                    try? await Task.sleep(for: .seconds(5))
                }
            }
            .onDisappear {
                leavePredictions()
            }
            .withScenePhaseHandlers(
                onActive: {
                    if let predictionsByStop,
                       predictionsRepository
                       .shouldForgetPredictions(predictionCount: predictionsByStop.predictionQuantity()) {
                        self.predictionsByStop = nil
                    }
                    joinPredictions(stop)
                },
                onInactive: leavePredictions,
                onBackground: {
                    leavePredictions()
                    errorBannerVM.loadingWhenPredictionsStale = true
                }
            )
        }
    }

    func loadEverything() {
        loadGlobalData()
        fetchStopData(stop)
        loadPinnedRoutes()
    }

    @MainActor
    func activateGlobalListener() async {
        for await globalData in globalRepository.state {
            globalResponse = globalData
        }
    }

    func loadGlobalData() {
        Task(priority: .high) {
            await activateGlobalListener()
        }
        Task {
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "LegacyStopDetailsPage.loadGlobalData",
                getData: { try await globalRepository.getGlobalData() },
                onRefreshAfterError: loadEverything
            )
        }
    }

    func loadPinnedRoutes() {
        Task {
            do {
                pinnedRoutes = try await pinnedRouteRepository.getPinnedRoutes()
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // getPinnedRoutes shouldn't actually fail
                debugPrint(error)
            }
        }
    }

    func togglePinnedRoute(_ routeId: String) {
        Task {
            do {
                _ = try await togglePinnedUsecase.execute(route: routeId)
                loadPinnedRoutes()
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // execute shouldn't actually fail
                debugPrint(error)
            }
        }
    }

    func changeStop(_ stop: Stop) {
        leavePredictions()
        fetchStopData(stop)
    }

    func fetchStopData(_ stop: Stop) {
        getSchedule(stop)
        joinPredictions(stop)
        updateDepartures(stop)
    }

    func getSchedule(_ stop: Stop) {
        Task {
            schedulesResponse = nil
            await fetchApi(
                errorBannerVM.errorRepository,
                errorKey: "LegacyStopDetailsPage.getSchedule",
                getData: { try await schedulesRepository.getSchedule(stopIds: [stop.id]) },
                onSuccess: { schedulesResponse = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    func joinPredictions(_ stop: Stop) {
        // no error handling since persistent errors cause stale predictions
        predictionsRepository.connectV2(stopIds: [stop.id], onJoin: { outcome in
            DispatchQueue.main.async {
                if case let .ok(result) = onEnum(of: outcome) {
                    predictionsByStop = result.data
                    checkPredictionsStale()
                }
                errorBannerVM.loadingWhenPredictionsStale = false
            }
        }, onMessage: { outcome in
            DispatchQueue.main.async {
                if case let .ok(result) = onEnum(of: outcome) {
                    if let existingPredictionsByStop = predictionsByStop {
                        predictionsByStop = existingPredictionsByStop.mergePredictions(updatedPredictions: result.data)
                    } else {
                        predictionsByStop = PredictionsByStopJoinResponse(
                            predictionsByStop: [result.data.stopId: result.data.predictions],
                            trips: result.data.trips,
                            vehicles: result.data.vehicles
                        )
                    }
                    checkPredictionsStale()
                }
                errorBannerVM.loadingWhenPredictionsStale = false
            }

        })
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    private func checkPredictionsStale() {
        if let lastPredictions = predictionsRepository.lastUpdated {
            errorBannerVM.errorRepository.checkPredictionsStale(
                predictionsLastUpdated: lastPredictions,
                predictionQuantity: Int32(
                    predictionsByStop?.predictionQuantity() ??
                        0
                ),
                action: {
                    leavePredictions()
                    joinPredictions(stop)
                }
            )
        }
    }

    func updateDepartures(
        _ stop: Stop? = nil,
        _ predictionsByStop: PredictionsByStopJoinResponse? = nil
    ) {
        let stop = stop ?? self.stop
        let predictionsByStop = predictionsByStop ?? self.predictionsByStop

        let targetPredictions = predictionsByStop?.toPredictionsStreamDataResponse()

        let newDepartures: StopDetailsDepartures? = if let globalResponse {
            StopDetailsDepartures.companion.fromData(
                stop: stop,
                global: globalResponse,
                schedules: schedulesResponse,
                predictions: targetPredictions,
                alerts: nearbyVM.alerts,
                pinnedRoutes: pinnedRoutes,
                filterAtTime: now.toKotlinInstant(),
                useTripHeadsigns: nearbyVM.tripHeadsignsEnabled
            )
        } else {
            nil
        }
        if filter == nil, let newFilter = newDepartures?.autoStopFilter() {
            nearbyVM.setLastStopDetailsFilter(stop.id, newFilter)
        }

        internalDepartures = newDepartures
        nearbyVM.setDepartures(stop.id, newDepartures)
    }
}
