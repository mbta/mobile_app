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

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        globalRepository: IGlobalRepository = RepositoryDI().global,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: Binding<StopDetailsFilter?>,
        nearbyVM: NearbyViewModel
    ) {
        self.globalRepository = globalRepository
        self.schedulesRepository = schedulesRepository
        self.predictionsRepository = predictionsRepository
        self.viewportProvider = viewportProvider
        self.stop = stop
        _filter = filter
        self.nearbyVM = nearbyVM
    }

    var body: some View {
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

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    func updateDepartures(_ stop: Stop? = nil) {
        let stop = stop ?? self.stop

        let newDepartures: StopDetailsDepartures? = if let globalResponse {
            StopDetailsDepartures(
                stop: stop,
                global: globalResponse,
                schedules: schedulesResponse,
                predictions: predictions,
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
