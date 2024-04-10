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
    @StateObject var scheduleFetcher: ScheduleFetcher
    @StateObject var predictionsFetcher: PredictionsFetcher
    var stop: Stop
    @Binding var filter: StopDetailsFilter?
    @State var now = Date.now

    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    init(backend: any BackendProtocol, socket: any PhoenixSocket, globalFetcher: GlobalFetcher,
         stop: Stop, filter: Binding<StopDetailsFilter?>) {
        self.globalFetcher = globalFetcher
        _scheduleFetcher = StateObject(wrappedValue: ScheduleFetcher(backend: backend))
        _predictionsFetcher = StateObject(wrappedValue: PredictionsFetcher(socket: socket))
        self.stop = stop
        _filter = filter
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
                ), now: now.toKotlinInstant(), filter: $filter)
            } else {
                ProgressView()
            }
        }
        .navigationTitle(stop.name)
        .onAppear {
            getSchedule()
            joinPredictions()
        }
        .onReceive(timer) { input in
            now = input
        }
        .onDisappear {
            leavePredictions()
        }
    }

    func getSchedule() {
        Task {
            await scheduleFetcher.getSchedule(stopIds: [stop.id])
        }
    }

    func joinPredictions() {
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
