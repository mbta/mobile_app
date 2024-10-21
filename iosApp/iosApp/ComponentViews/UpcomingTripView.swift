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
                Text(overridden.textWithLocale()).realtime()
            case .hidden, .skipped:
                // should have been filtered out already
                Text(verbatim: "")
            case .now:
                Text("Now")
                    .font(Typography.headlineBold)
                    .realtime()
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .arrivingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.arrivingOther())
            case .boarding:
                Text("BRD", comment: "Shorthand for boarding")
                    .font(Typography.headlineBold)
                    .realtime()
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .boardingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.boardingOther())
            case .arriving:
                Text("ARR", comment: "Shorthand for arriving")
                    .font(Typography.headlineBold)
                    .realtime()
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .arrivingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.arrivingOther())
            case .approaching:
                PredictionText(minutes: 1).realtime()
            case let .time(format):
                Text(Date(instant: format.predictionTime), style: .time)
                    .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                    .realtime()
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.distantFutureFirst(
                            date: format.predictionTime.toNSDate(),
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters
                        .distantFutureOther(date: format.predictionTime.toNSDate()))
            case let .minutes(format):
                PredictionText(minutes: format.minutes)
                    .realtime()
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.predictionMinutesFirst(
                            minutes: format.minutes,
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters.predictionMinutesOther(minutes: format.minutes))
            case let .scheduleTime(format):
                Text(format.scheduledTime.toNSDate(), style: .time)
                    .opacity(0.6)
                    .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.scheduleTimeFirst(
                            date: format.scheduledTime.toNSDate(),
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters.scheduleTimeOther(date: format.scheduledTime.toNSDate()))
            case let .scheduleMinutes(format):
                PredictionText(minutes: format.minutes)
                    .opacity(0.6)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.scheduleMinutesFirst(
                            minutes: format.minutes,
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters.scheduleMinutesOther(minutes: format.minutes))
            case let .cancelled(format):
                HStack(spacing: Self.subjectSpacing) {
                    Text("Cancelled")
                        .font(Typography.footnote)
                        .foregroundStyle(Color.deemphasized)
                    Text(format.scheduledTime.toNSDate(), style: .time)
                        .font(Typography.footnoteSemibold)
                        .strikethrough()
                        .foregroundStyle(Color.deemphasized)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(isFirst
                    ? accessibilityFormatters.scheduleTimeFirst(
                        date: format.scheduledTime.toNSDate(),
                        vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                    )
                    : accessibilityFormatters.scheduleTimeOther(date: format.scheduledTime.toNSDate()))
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
        Text("\(vehicleText) boarding now",
             comment: """
             Describe that a vehicle is boarding now, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry). For example, 'bus boarding now'
             """)
    }

    public func boardingOther() -> Text {
        Text("and boarding now",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
              For example, '[bus arriving in 1 minute], and boarding now'
             """)
    }

    public func arrivingFirst(vehicleText: String) -> Text {
        Text("\(vehicleText) arriving now",
             comment: """
             Describe that a vehicle is arriving now, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry). For example, 'bus arriving now'
             """)
    }

    public func arrivingOther() -> Text {
        Text("and arriving now",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
              For example, '[bus arriving in 1 minute], and arriving now'
             """)
    }

    public func distantFutureFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date))",
             comment: """
             Describe the time at which a vehicle will arrive, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
             For example, 'bus arriving at 10:30AM'
             """)
    }

    public func distantFutureOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date))",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
             For example, '[bus arriving at 10:30AM], and at 10:45 AM'
             """)
    }

    public func scheduleTimeFirst(date: Date, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving at \(timeFormatter.string(from: date)) scheduled",
             comment: """
             Describe the time at which a vehicle is scheduled to arrive, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry), second is the clock time it will arrive.
             For example, 'bus arriving at 10:30AM scheduled'
             "")
    }

    public func scheduleTimeOther(date: Date) -> Text {
        Text("and at \(timeFormatter.string(from: date)) scheduled",
             comment: """
             The second or more arrival in a list of scheduled upcoming arrivals read aloud for VoiceOver users.
             For example, '[bus arriving at 10:30AM scheduled], and at 10:45 AM scheduled'
             """)
    }

    public func scheduleMinutesFirst(minutes: Int32, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving in \(minutes) min scheduled")
    }

    public func scheduleMinutesOther(minutes: Int32) -> Text {
        Text("and in \(minutes) min scheduled")
    }

    public func predictionMinutesFirst(minutes: Int32, vehicleText: String) -> Text {
        Text("\(vehicleText) arriving in \(minutes) min",
             comment: """
             Describe the number of minutes until a vehicle will arrive, as read aloud for VoiceOver users.
             First value is the type of vehicle (bus, train, ferry), second is the number of minutes until it arrives
             For example, 'bus arriving in 5 minutes'
             """)
    }

    public func predictionMinutesOther(minutes: Int32) -> Text {
        Text("and in \(minutes) min",
             comment: """
             The second or more arrival in a list of upcoming arrivals read aloud for VoiceOver users.
             For example, '[bus arriving in 5 minutes], and in 10 minutes'
             """)
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
