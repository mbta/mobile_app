//
//  UpcomingTripView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

extension TripInstantDisplay.Overridden {
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
        case noSchedulesToday
        case serviceEndedToday
        case noService(shared.Alert.Effect)
        case some(TripInstantDisplay)
    }

    var body: some View {
        predictionView
            .foregroundStyle(Color.text)
            .frame(minWidth: 48, alignment: .trailing)
            .padding(.trailing, 4)
    }

    @ViewBuilder
    var predictionView: some View {
        switch prediction {
        case let .some(prediction):
            switch onEnum(of: prediction) {
            case let .overridden(overridden):
                Text(overridden.textWithLocale())
            case .hidden, .skipped:
                // should have been filtered out already
                Text(verbatim: "")
            case .now:
                Text("Now").font(Typography.headlineBold)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .arrivingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.arrivingOther())
            case .boarding:
                Text("BRD").font(Typography.headlineBold)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .boardingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.boardingOther())
            case .arriving:
                Text("ARR").font(Typography.headlineBold)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .arrivingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.arrivingOther())
            case .approaching:
                PredictionText(minutes: 1)
            case let .asTime(format):
                Text(Date(instant: format.predictionTime), style: .time)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.distantFutureFirst(
                            date: format.predictionTime.toNSDate(),
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters
                        .distantFutureOther(date: format.predictionTime.toNSDate()))
                    .font(Typography.footnoteSemibold)
            case let .schedule(schedule):
                HStack(spacing: Self.subjectSpacing) {
                    Text(schedule.scheduleTime.toNSDate(), style: .time)
                        .accessibilityLabel(isFirst
                            ? accessibilityFormatters.scheduledFirst(
                                date: schedule.scheduleTime.toNSDate(),
                                vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                            )
                            : accessibilityFormatters
                            .scheduledOther(date: schedule.scheduleTime.toNSDate()))
                        .font(Typography.footnoteSemibold)
                    Image(.faClock)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: iconSize, height: iconSize)
                        .padding(4)
                        .foregroundStyle(Color.deemphasized)
                }
            case let .minutes(format):
                PredictionText(minutes: format.minutes)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.predictionMinutesFirst(minutes: format.minutes,
                                                                         vehicleText: routeType?
                                                                             .typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.predictionMinutesOther(minutes: format.minutes))
            case let .cancelled(schedule):
                HStack(spacing: Self.subjectSpacing) {
                    Text("Cancelled")
                        .font(Typography.footnote)
                        .foregroundStyle(Color.deemphasized)
                    Text(schedule.scheduledTime.toNSDate(), style: .time)
                        .font(Typography.footnoteSemibold)
                        .strikethrough()
                        .foregroundStyle(Color.deemphasized)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    isFirst
                        ? accessibilityFormatters.scheduledFirst(
                            date: schedule.scheduledTime.toNSDate(),
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters.scheduledOther(date: schedule.scheduledTime.toNSDate())
                )
            }
        case let .noService(alertEffect):
            NoServiceView(effect: .from(alertEffect: alertEffect))
        case .none:
            Text("Predictions unavailable").font(Typography.footnote)
        case .serviceEndedToday:
            Text("Service ended").font(Typography.footnote)
        case .noSchedulesToday:
            Text("No service today").font(Typography.footnote)
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

    public func predictionTimeFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date))")
    }

    public func predictionTimeOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date))")
    }

    public func cancelledFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date)) cancelled")
    }

    public func cancelledOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date)) cancelled")
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
        ViewThatFits(in: .horizontal) {
            HStack {
                fullText
                    .lineLimit(1)
                fullImage
            }
            VStack(alignment: .trailing) {
                fullText
                    .lineLimit(1)
                fullImage
            }
            HStack {
                fullText
                fullImage
            }
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
        case .detour: Image(systemName: "exclamationmark.triangle.fill")
        case .shuttle: Image(.modeBus)
        case .stopClosed: Image(systemName: "xmark.octagon.fill")
        case .suspension: Image(systemName: "exclamationmark.triangle.fill")
        case .unknown: Image(systemName: "questionmark.circle.fill")
        }
    }

    var fullText: some View {
        rawText
            .font(Typography.footnote)
            .textCase(.uppercase)
    }

    var fullImage: some View {
        rawImage
            .resizable()
            .scaledToFill()
            .foregroundStyle(Color.deemphasized)
            .frame(width: iconSize, height: iconSize)
            .padding(2)
    }
}

struct UpcomingTripView_Previews: PreviewProvider {
    static var previews: some View {
        VStack(alignment: .trailing) {
            UpcomingTripView(prediction: .noService(.suspension), routeType: .heavyRail)
            UpcomingTripView(prediction: .noService(.shuttle), routeType: .heavyRail)
            UpcomingTripView(prediction: .noService(.stopClosure), routeType: .heavyRail)
            UpcomingTripView(prediction: .noService(.detour), routeType: .heavyRail)
            UpcomingTripView(prediction: .noService(.detour), routeType: .heavyRail)
        }
        .padding(8)
        .frame(maxWidth: 150)
        .background(Color.fill1)
        .previewDisplayName("No Service")
    }
}
