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
    var analytics: StopDetailsAnalytics = AnalyticsProvider.shared
    let globalRepository: IGlobalRepository
    @State var globalResponse: GlobalResponse?
    @ObservedObject var viewportProvider: ViewportProvider
    let schedulesRepository: ISchedulesRepository
    var settingsRepository = RepositoryDI().settings
    @State var schedulesResponse: ScheduleResponse?
    var pinnedRouteRepository = RepositoryDI().pinnedRoutes
    var togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase

    @State var predictionsRepository: IPredictionsRepository
    var stop: Stop
    @State var filter: StopDetailsFilter?
    @State var departures: StopDetailsDepartures? = nil
    @State var now = Date.now
    @ObservedObject var nearbyVM: NearbyViewModel
    @State var pinnedRoutes: Set<String> = []
    @State var predictions: PredictionsStreamDataResponse?
    @State var predictionsByStop: PredictionsByStopJoinResponse?
    @State var predictionsV2Enabled = false
    var errorBannerRepository: IErrorBannerStateRepository

    let inspection = Inspection<Self>()

    var didAppear: ((Self) -> Void)?

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        settingsRepository: ISettingsRepository = RepositoryDI().settings,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        errorBannerRepository: IErrorBannerStateRepository = RepositoryDI().errorBanner,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: StopDetailsFilter?,
        nearbyVM: NearbyViewModel,
        predictionsV2Enabled: Bool = false
    ) {
        self.globalRepository = globalRepository
        self.schedulesRepository = schedulesRepository
        self.settingsRepository = settingsRepository
        self.predictionsRepository = predictionsRepository
        self.errorBannerRepository = errorBannerRepository
        self.viewportProvider = viewportProvider
        self.stop = stop
        // initialize filter to value it is passed. this will not be re-set on subsequent inits
        self.filter = filter
        self.nearbyVM = nearbyVM
        self.predictionsV2Enabled = predictionsV2Enabled
    }

    var body: some View {
        VStack {
            if predictionsV2Enabled {
                Text("Using Predictions Channel V2")
            }

            StopDetailsView(
                stop: stop,
                filter: $filter,
                departures: departures,
                nearbyVM: nearbyVM,
                pinnedRoutes: pinnedRoutes,
                togglePinnedRoute: togglePinnedRoute
            )
            .onAppear {
                loadGlobalData()
                fetchStopData(stop)
                loadPinnedRoutes()
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
                updateDepartures(stop, newPredictionsByStop, predictions)
            }
            .onChange(of: predictions) { _ in

                updateDepartures(stop)
            }
            .onChange(of: schedulesResponse) { _ in

                updateDepartures(stop)
            }
            .onChange(of: filter) { newFilter in
                nearbyVM.setLastStopDetailsFilter(stop.id, newFilter)
            }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .task(id: stop.id) {
                while !Task.isCancelled {
                    now = Date.now
                    updateDepartures()
                    await checkPredictionsStale()
                    try? await Task.sleep(for: .seconds(5))
                }
            }
            .onDisappear {
                leavePredictions()
            }
            .withScenePhaseHandlers(onActive: { joinPredictions(stop) },
                                    onInactive: leavePredictions,
                                    onBackground: leavePredictions)
        }
    }

    func loadGlobalData() {
        Task {
            do {
                globalResponse = try await globalRepository.getGlobalData()
            } catch {
                debugPrint(error)
            }
        }
    }

    func loadPinnedRoutes() {
        Task {
            do {
                pinnedRoutes = try await pinnedRouteRepository.getPinnedRoutes()
            } catch {
                debugPrint(error)
            }
        }
    }

    func togglePinnedRoute(_ routeId: String) {
        Task {
            do {
                _ = try await togglePinnedUsecase.execute(route: routeId)
                loadPinnedRoutes()
            } catch {
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
            do {
                schedulesResponse = try await schedulesRepository
                    .getSchedule(stopIds: [stop.id])
            } catch {}
        }
    }

    func joinPredictions(_ stop: Stop) {
        Task {
            let settings = try await settingsRepository.getSettings()
            var isEnabled = settings.first(where: { $0.key == .predictionsV2Channel })?.isOn ?? false
            predictionsV2Enabled = isEnabled
            if isEnabled {
                joinPredictionsV2(stopIds: [stop.id])
            } else {
                predictionsRepository.connect(stopIds: [stop.id]) { outcome in

                    DispatchQueue.main.async {
                        switch onEnum(of: outcome) {
                        case let .ok(result): predictions = result.data
                        case .error: predictions = nil
                        }
                    }
                }
            }
        }
    }

    func joinPredictionsV2(stopIds: Set<String>) {
        predictionsRepository.connectV2(stopIds: Array(stopIds), onJoin: { outcome in
            DispatchQueue.main.async {
                if case let .ok(result) = onEnum(of: outcome) {
                    predictionsByStop = result.data
                }
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
                }
            }

        })
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    private func checkPredictionsStale() async {
        if let lastPredictions = predictionsRepository.lastUpdated {
            errorBannerRepository.checkPredictionsStale(
                predictionsLastUpdated: lastPredictions,
                predictionQuantity: Int32(
                    predictionsByStop?.predictionQuantity() ??
                        predictions?.predictionQuantity() ??
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
        _ predictionsByStop: PredictionsByStopJoinResponse? = nil,
        _ predictions: PredictionsStreamDataResponse? = nil
    ) {
        let stop = stop ?? self.stop
        let predictionsByStop = predictionsByStop ?? self.predictionsByStop

        let targetPredictions = if let predictionsByStop {
            predictionsByStop.toPredictionsStreamDataResponse()
        } else {
            predictions ?? self.predictions
        }

        let newDepartures: StopDetailsDepartures? = if let globalResponse {
            StopDetailsDepartures(
                stop: stop,
                global: globalResponse,
                schedules: schedulesResponse,
                predictions: targetPredictions,
                alerts: nearbyVM.alerts,
                pinnedRoutes: pinnedRoutes,
                filterAtTime: now.toKotlinInstant()
            )
        } else {
            nil
        }

        if filter == nil, let newFilter = newDepartures?.autoFilter() {
            filter = newFilter
        }

        departures = newDepartures
        nearbyVM.setDepartures(stop.id, newDepartures)
    }
}
