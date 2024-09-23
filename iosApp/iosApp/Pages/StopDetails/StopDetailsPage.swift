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
    @Binding var filter: StopDetailsFilter?
    @State var now = Date.now
    @ObservedObject var nearbyVM: NearbyViewModel
    @State var pinnedRoutes: Set<String> = []
    @State var predictions: PredictionsStreamDataResponse?
    @State var predictionsByStop: PredictionsByStopJoinResponse?
    @State var predictionsV2Enabled = false

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        settingsRepository: ISettingsRepository = RepositoryDI().settings,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: Binding<StopDetailsFilter?>,
        nearbyVM: NearbyViewModel,
        predictionsV2Enabled: Bool = false
    ) {
        self.globalRepository = globalRepository
        self.schedulesRepository = schedulesRepository
        self.settingsRepository = settingsRepository
        self.predictionsRepository = predictionsRepository
        self.viewportProvider = viewportProvider
        self.stop = stop
        _filter = filter
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
                nearbyVM: nearbyVM,
                pinnedRoutes: pinnedRoutes,
                togglePinnedRoute: togglePinnedRoute
            )
            .onAppear {
                loadGlobalData()
                changeStop(stop)
                loadPinnedRoutes()
            }
            .onChange(of: stop) { nextStop in changeStop(nextStop) }
            .onChange(of: globalResponse) { _ in updateDepartures() }
            .onChange(of: pinnedRoutes) { _ in updateDepartures() }
            .onChange(of: predictionsByStop) { _ in
                updateDepartures()
            }
            .onChange(of: predictions) { _ in updateDepartures() }
            .onChange(of: schedulesResponse) { _ in updateDepartures() }
            .onReceive(inspection.notice) { inspection.visit(self, $0) }
            .onReceive(timer) { input in
                now = input
                updateDepartures()
            }
            .onDisappear { leavePredictions() }
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
                        predictions = if let data = outcome.data {
                            data
                        } else {
                            nil
                        }
                    }
                }
            }
        }
    }

    func joinPredictionsV2(stopIds: Set<String>) {
        predictionsRepository.connectV2(stopIds: Array(stopIds), onJoin: { outcome in
            DispatchQueue.main.async {
                if let data = outcome.data {
                    predictionsByStop = data
                }
            }
        }, onMessage: { outcome in
            DispatchQueue.main.async {
                if let data = outcome.data {
                    if let existingPredictionsByStop = predictionsByStop {
                        predictionsByStop = PredictionsByStopJoinResponse.companion
                            .mergePredictions(allByStop: existingPredictionsByStop, updatedPredictions: data)
                    } else {
                        predictionsByStop = PredictionsByStopJoinResponse(
                            predictionsByStop: [data.stopId: data.predictions],
                            trips: data.trips,
                            vehicles: data.vehicles
                        )
                    }
                }
            }

        })
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    func updateDepartures(_ stop: Stop? = nil) {
        let stop = stop ?? self.stop

        let targetPredictions = if let predictionsByStop {
            PredictionsByStopJoinResponse.companion
                .toPredictionsStreamDataResponse(predictionsByStop: predictionsByStop)
        } else {
            predictions
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

        nearbyVM.setDepartures(newDepartures)
    }
}
