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

    @StateObject var scheduleFetcher: ScheduleFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    var stop: Stop
    @Binding var filter: StopDetailsFilter?
    @State var now = Date.now
    @State var departures: StopDetailsDepartures?
    @State var servedRoutes: [Route] = []

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        backend: any BackendProtocol,
        socket: any PhoenixSocket,
        globalFetcher: GlobalFetcher,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: Binding<StopDetailsFilter?>
    ) {
        self.globalFetcher = globalFetcher
        self.viewportProvider = viewportProvider
        _scheduleFetcher = StateObject(wrappedValue: ScheduleFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        self.stop = stop
        _filter = filter
    }

    var body: some View {
        VStack {
            StopDetailsRoutePills(servedRoutes: servedRoutes, tapRoutePill: tapRoutePill, filter: $filter)
            clearFilterButton
            departureHeader
            if let departures {
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
        .onChange(of: scheduleFetcher.schedules) { _ in updateDepartures() }
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

    @ViewBuilder
    private var departureHeader: some View {
        if predictionsFetcher.predictions != nil {
            Text("Live departures")
        } else if scheduleFetcher.schedules != nil {
            Text("Scheduled departures")
        } else {
            Text("Departures")
        }
    }

    func changeStop(_ stop: Stop) {
        getSchedule(stop)
        joinPredictions(stop)
        updateDepartures(stop)
        viewportProvider.animateTo(coordinates: stop.coordinate)
    }

    func getSchedule(_ stop: Stop) {
        Task {
            await scheduleFetcher.getSchedule(stopIds: [stop.id])
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
        guard let departures else { return }
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
        departures = if let globalResponse = globalFetcher.response {
            StopDetailsDepartures(
                stop: stop,
                global: globalResponse,
                schedules: scheduleFetcher.schedules,
                predictions: predictionsFetcher.predictions,
                filterAtTime: now.toKotlinInstant()
            )
        } else {
            nil
        }
        if let departures {
            servedRoutes = Set(departures.routes.map { pattern in pattern.route })
                .sorted { $0.sortOrder < $1.sortOrder }
        }
    }
}
