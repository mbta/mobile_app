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
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var viewportProvider: ViewportProvider
    let schedulesRepository: ISchedulesRepository
    @State var schedulesResponse: ScheduleResponse?
    @StateObject var predictionsFetcher: PredictionsFetcher
    var stop: Stop
    @Binding var filter: StopDetailsFilter?
    @State var now = Date.now
    @State var servedRoutes: [Route] = []
    @ObservedObject var nearbyVM: NearbyViewModel

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        socket: any PhoenixSocket,
        globalFetcher: GlobalFetcher,
        schedulesRepository: ISchedulesRepository = RepositoryDI().schedules,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: Binding<StopDetailsFilter?>,
        nearbyVM: NearbyViewModel
    ) {
        self.globalFetcher = globalFetcher
        self.schedulesRepository = schedulesRepository
        self.viewportProvider = viewportProvider
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        self.stop = stop
        _filter = filter
        self.nearbyVM = nearbyVM
    }

    var body: some View {
        VStack {
            StopDetailsRoutePills(servedRoutes: servedRoutes, tapRoutePill: tapRoutePill, filter: $filter)
            clearFilterButton
            departureHeader
            if let departures = nearbyVM.departures {
                StopDetailsRoutesView(departures: departures, now: now.toKotlinInstant(), filter: $filter)
            } else {
                ProgressView()
            }
        }
        .navigationTitle(stop.name)
        .onAppear { changeStop(stop) }
        .onChange(of: stop) { nextStop in changeStop(nextStop) }
        .onChange(of: globalFetcher.response) { _ in updateDepartures() }
        .onChange(of: predictionsFetcher.predictions) { _ in updateDepartures() }
        .onChange(of: schedulesResponse) { _ in updateDepartures() }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onReceive(timer) { input in
            now = input
            updateDepartures()
        }
        .onDisappear { leavePredictions() }
    }

    @ViewBuilder
    private var clearFilterButton: some View {
        if filter != nil {
            Button(action: { filter = nil }, label: { Text("Clear Filter") })
        }
    }

    private var departureHeader: some View {
        if predictionsFetcher.predictions != nil {
            Text("Live departures")
        } else if schedulesResponse != nil {
            Text("Scheduled departures")
        } else {
            Text("Departures")
        }
    }

    func changeStop(_ stop: Stop) {
        getSchedule(stop)
        joinPredictions(stop)
        updateDepartures(stop)
    }

    func getSchedule(_ stop: Stop) {
        Task {
            do {
                schedulesResponse = try await schedulesRepository
                    .getSchedule(stopIds: [stop.id])
            } catch {}
        }
    }

    func joinPredictions(_ stop: Stop) {
        Task {
            predictionsFetcher.run(stopIds: [stop.id])
        }
    }

    func leavePredictions() {
        Task {
            predictionsFetcher.leave()
        }
    }

    func tapRoutePill(_ route: Route) {
        if filter?.routeId == route.id { return }
        guard let departures = nearbyVM.departures else { return }
        let patterns = departures.routes.first { patterns in patterns.route.id == route.id }
        if patterns == nil { return }
        let defaultDirectionId = patterns?.patternsByHeadsign.flatMap { headsign in
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
                predictions: predictionsFetcher.predictions,
                filterAtTime: now.toKotlinInstant()
            )
        } else {
            nil
        }

        nearbyVM.setDepartures(newDepartures)
        if let departures = nearbyVM.departures {
            servedRoutes = Set(departures.routes.map { pattern in pattern.route })
                .sorted { $0.sortOrder < $1.sortOrder }
        }
    }
}
