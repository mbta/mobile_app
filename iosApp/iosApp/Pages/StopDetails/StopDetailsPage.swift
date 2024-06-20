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
    var analytics: StopDetailsAnalytics = AnalyticsProvider()
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var viewportProvider: ViewportProvider
    let schedulesRepository: ISchedulesRepository
    @State var schedulesResponse: ScheduleResponse?
    var pinnedRouteRepository = RepositoryDI().pinnedRoutes
    var togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase

    let predictionsRepository: IPredictionsRepository
    var stop: Stop
    @Binding var filter: StopDetailsFilter?
    @State var now = Date.now
    @State var servedRoutes: [(route: Route, line: Line?)] = []
    @ObservedObject var nearbyVM: NearbyViewModel
    @State var pinnedRoutes: Set<String> = []
    @State var predictions: PredictionsStreamDataResponse?

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        globalFetcher: GlobalFetcher,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        predictionsRepository: IPredictionsRepository = RepositoryDI().predictions,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: Binding<StopDetailsFilter?>,
        nearbyVM: NearbyViewModel
    ) {
        self.globalFetcher = globalFetcher
        self.schedulesRepository = schedulesRepository
        self.predictionsRepository = predictionsRepository
        self.viewportProvider = viewportProvider
        self.stop = stop
        _filter = filter
        self.nearbyVM = nearbyVM
    }

    var body: some View {
        ZStack {
            Color.fill2.ignoresSafeArea(.all)
            VStack(spacing: 0) {
                VStack {
                    SheetHeader(onClose: { nearbyVM.goBack() }, title: stop.name)
                    StopDetailsRoutePills(servedRoutes: servedRoutes, tapRoutePill: tapRoutePill, filter: $filter)
                }
                .padding([.bottom], 8)
                .border(Color.halo.opacity(0.15), width: 2)

                if let departures = nearbyVM.departures {
                    StopDetailsRoutesView(
                        departures: departures,
                        now: now.toKotlinInstant(),
                        filter: $filter,
                        pushNavEntry: nearbyVM.pushNavEntry,
                        pinRoute: togglePinnedRoute,
                        pinnedRoutes: pinnedRoutes
                    ).frame(maxHeight: .infinity)
                } else {
                    ProgressView()
                }
            }
        }
        .onAppear {
            changeStop(stop)
            loadPinnedRoutes()
        }
        .onChange(of: stop) { nextStop in changeStop(nextStop) }
        .onChange(of: globalFetcher.response) { _ in updateDepartures() }
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
                try await togglePinnedUsecase.execute(route: routeId)
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

    func tapRoutePill(_ route: Route) {
        if filter?.routeId == route.id { filter = nil; return }
        guard let departures = nearbyVM.departures else { return }
        guard let patterns = departures.routes.first(where: { patterns in patterns.route.id == route.id })
        else { return }
        analytics.tappedRouteFilter(routeId: patterns.route.id, stopId: stop.id)
        let defaultDirectionId = patterns.patternsByHeadsign.flatMap { headsign in
            headsign.patterns.map { pattern in pattern.directionId }
        }.min() ?? 0
        filter = .init(routeId: route.id, directionId: defaultDirectionId)
    }

    func updateDepartures(_ stop: Stop? = nil) {
        let stop = stop ?? self.stop
        servedRoutes = []

        let newDepartures: StopDetailsDepartures? = if let globalResponse = globalFetcher.response {
            StopDetailsDepartures(
                stop: stop,
                global: globalResponse,
                schedules: schedulesResponse,
                predictions: predictions,
                filterAtTime: now.toKotlinInstant()
            )
        } else {
            nil
        }

        nearbyVM.setDepartures(newDepartures)
        if let departures = nearbyVM.departures {
            servedRoutes = Set(departures.routes.map { pattern in pattern.route })
                .sorted { $0.sortOrder < $1.sortOrder }
                .map { (route: $0, line: globalFetcher.lookUpLine(lineId: $0.lineId)) }
        }
    }
}
