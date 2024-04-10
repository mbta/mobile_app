//
//  StopDetailsPage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-28.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftPhoenixClient
import SwiftUI

struct StopDetailsPage: View {
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var viewportProvider: ViewportProvider

    @StateObject var scheduleFetcher: ScheduleFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    var stop: Stop
    var filter: StopDetailsFilter?
    @State var now = Date.now

    let inspection = Inspection<Self>()
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(
        backend: any BackendProtocol,
        socket: any PhoenixSocket,
        globalFetcher: GlobalFetcher,
        viewportProvider: ViewportProvider,
        stop: Stop,
        filter: StopDetailsFilter?
    ) {
        self.globalFetcher = globalFetcher
        self.viewportProvider = viewportProvider
        _scheduleFetcher = StateObject(wrappedValue: ScheduleFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        self.stop = stop
        self.filter = filter
    }

    var body: some View {
        VStack {
            if predictionsFetcher.predictions != nil {
                Text("Live departures")
            } else if scheduleFetcher.schedules != nil {
                Text("Scheduled departures")
            } else {
                Text("Departures")
            }
            if let globalResponse = globalFetcher.response {
                StopDetailsRoutesView(departures: StopDetailsDepartures(
                    stop: stop,
                    global: globalResponse,
                    schedules: scheduleFetcher.schedules,
                    predictions: predictionsFetcher.predictions,
                    filterAtTime: now.toKotlinInstant()
                ), now: now.toKotlinInstant())
            } else {
                ProgressView()
            }
        }
        .navigationTitle(stop.name)
        .onAppear { changeStop(stop) }
        .onChange(of: stop) { nextStop in changeStop(nextStop) }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onReceive(timer) { input in now = input }
        .onDisappear { leavePredictions() }
    }

    func changeStop(_ stop: Stop) {
        getSchedule(stop)
        joinPredictions(stop)
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
}
