//
//  UpcomingTripView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

extension UpcomingTrip.FormatOverridden {
    func textWithLocale() -> AttributedString {
        var result = AttributedString(text)
        result.languageIdentifier = "en-US"
        return result
    }
}

struct UpcomingTripView: View {
    let prediction: State
    var routeType: RouteType?
    var isFirst: Bool = true
    var isOnly: Bool = true

    let accessibilityFormatters = UpcomingTripAccessibilityFormatters()

    private static let subjectSpacing: CGFloat = 4
    @ScaledMetric private var iconSize: CGFloat = 16

    enum State: Equatable {
        case loading
        case none
        case noService(shared.Alert.Effect)
        case some(UpcomingTrip.Format)
    }

    var body: some View {
        predictionView
            .foregroundStyle(Color.text)
            .frame(minWidth: 48, alignment: .trailing)
            .padding(.trailing, 4)
    }

    var vehicleTypeText: String {
        // hardcoding plurals because pluralized strings that don't include the number are not supported
        // https://developer.apple.com/forums/thread/737329#737329021
        switch routeType {
        case .bus:
            isOnly ? NSLocalizedString("bus", comment: "bus") : NSLocalizedString("buses", comment: "buses")

        case .commuterRail, .heavyRail, .lightRail:
            isOnly ? NSLocalizedString("train", comment: "train") : NSLocalizedString("trains", comment: "trains")

        case .ferry: isOnly ? NSLocalizedString("ferry", comment: "ferry")
            : NSLocalizedString("ferries", comment: "ferries")
        case nil: ""
        }
    }

    @ViewBuilder
    var predictionView: some View {
        switch prediction {
        case let .some(prediction):
            switch onEnum(of: prediction) {
            case let .overridden(overridden):
                Text(overridden.textWithLocale())
            case .hidden:
                // should have been filtered out already
                Text(verbatim: "")
            case .boarding:
                Text("BRD").font(.headline).bold()
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.boardingFirst(vehicleText: vehicleTypeText)
                        : accessibilityFormatters.boardingOther())
            case .arriving:
                Text("ARR").font(.headline).bold()
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.arrivingFirst(vehicleText: vehicleTypeText)
                        : accessibilityFormatters.arrivingOther())
            case .approaching:
                PredictionText(minutes: 1)
            case let .distantFuture(format):
                Text(Date(instant: format.predictionTime), style: .time)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.distantFutureFirst(
                            date: format.predictionTime.toNSDate(),
                            vehicleText: vehicleTypeText
                        )
                        : accessibilityFormatters.distantFutureOther(date: format.predictionTime.toNSDate()))
                    .font(.footnote)
                    .fontWeight(.semibold)
            case let .schedule(schedule):
                if routeType == .commuterRail {
                    Text(schedule.scheduleTime.toNSDate(), style: .time)
                        .accessibilityLabel(isFirst
                            ? accessibilityFormatters.scheduledFirst(
                                date: schedule.scheduleTime.toNSDate(),
                                vehicleText: vehicleTypeText
                            )
                            : accessibilityFormatters.scheduledOther(date: schedule.scheduleTime.toNSDate()))
                        .font(.headline.weight(.regular))
                } else {
                    HStack(spacing: Self.subjectSpacing) {
                        Text(schedule.scheduleTime.toNSDate(), style: .time)
                            .accessibilityLabel(isFirst
                                ? accessibilityFormatters.scheduledFirst(
                                    date: schedule.scheduleTime.toNSDate(),
                                    vehicleText: vehicleTypeText
                                )
                                : accessibilityFormatters
                                .scheduledOther(date: schedule.scheduleTime.toNSDate()))
                            .font(.footnote)
                            .fontWeight(.semibold)
                        Image(.faClock)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: iconSize, height: iconSize)
                            .padding(4)
                            .foregroundStyle(Color.deemphasized)
                    }
                }
            case let .minutes(format):
                PredictionText(minutes: format.minutes)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.predictionMinutesFirst(minutes: format.minutes,
                                                                         vehicleText: vehicleTypeText)
                        : accessibilityFormatters.predictionMinutesOther(minutes: format.minutes))
            }
        case let .noService(alertEffect):
            NoServiceView(effect: .from(alertEffect: alertEffect))
        case .none:
            Text("No real-time data").font(.footnote)
        case .loading:
            ProgressView()
        }
    }
}

class UpcomingTripAccessibilityFormatters {
    private let timeFormatter: DateFormatter = makeTimeFormatter()

    public func boardingFirst(vehicleText: String) -> Text {
        Text("\(vehicleText) boarding now")
    }

    public func boardingOther() -> Text {
        Text("and boarding now")
    }

    public func arrivingFirst(vehicleText: String) -> Text {
        Text("\(vehicleText) arriving now")
    }

    public func arrivingOther() -> Text {
        Text("and arriving now")
    }

    public func distantFutureFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date))")
    }

    public func distantFutureOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date))")
    }

    public func scheduledFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date)) scheduled")
    }

    public func scheduledOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date)) scheduled")
    }

    public func predictionMinutesFirst(minutes: Int32, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving in \(minutes) min")
    }

    public func predictionMinutesOther(minutes: Int32) -> Text {
        Text("and in \(minutes) min")
    }
}

func makeTimeFormatter() -> DateFormatter {
    let formatter = DateFormatter()
    formatter.dateStyle = .none
    formatter.timeStyle = .short
    return formatter
}

struct NoServiceView: View {
    let effect: Effect

    @ScaledMetric private var iconSize: CGFloat = 20

    enum Effect {
        case detour
        case shuttle
        case stopClosed
        case suspension
        case unknown

        static func from(alertEffect: shared.Alert.Effect) -> Self {
            switch alertEffect {
            case .detour: .detour
            case .shuttle: .shuttle
            case .stationClosure, .stopClosure: .stopClosed
            case .suspension: .suspension
            default: .unknown
            }
        }
    }

    var body: some View {
        HStack {
            rawText
                .font(.footnote)
                .textCase(.uppercase)
            rawImage
                .resizable()
                .scaledToFill()
                .foregroundStyle(Color.deemphasized)
                .frame(width: iconSize, height: iconSize)
                .padding(2)
        }
    }

    var rawText: Text {
        switch effect {
        case .detour: Text("Detour")
        case .shuttle: Text("Shuttle")
            .accessibilityLabel(Text("Shuttle buses replace service"))
        case .stopClosed: Text("Stop Closed")
        case .suspension: Text("Suspension")
            .accessibilityLabel(Text("Service suspended"))
        case .unknown: Text("No Service")
        }
    }

    var rawImage: Image {
        switch effect {
        case .detour: Image(systemName: "circle.fill")
        case .shuttle: Image(.modeBus)
        case .stopClosed: Image(systemName: "xmark.octagon.fill")
        case .suspension: Image(systemName: "exclamationmark.triangle.fill")
        case .unknown: Image(systemName: "questionmark.circle.fill")
        }
    }
}

struct UpcomingTripView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(alignment: .trailing) {
            UpcomingTripView(prediction: .noService(.suspension), routeType: .heavyRail)
            UpcomingTripView(prediction: .noService(.shuttle), routeType: .heavyRail)
            UpcomingTripView(prediction: .noService(.stopClosure), routeType: .heavyRail)
            UpcomingTripView(prediction: .noService(.detour), routeType: .heavyRail)
        }
        .previewDisplayName("No Service")
    }
}
