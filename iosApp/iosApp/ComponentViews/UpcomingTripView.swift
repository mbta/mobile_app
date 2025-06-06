//
//  UpcomingTripView.swift
//  iosApp
//
//  Created by Simon, Emma on 3/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
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
    var hideRealtimeIndicators: Bool = false
    var isFirst: Bool = true
    var isOnly: Bool = true
    var maxTextAlpha: Double = 1.0

    let accessibilityFormatters = UpcomingTripAccessibilityFormatters()

    private static let subjectSpacing: CGFloat = 4
    @ScaledMetric private var iconSize: CGFloat = 16

    enum State: Equatable {
        case loading
        case noTrips(UpcomingFormat.NoTripsFormat)
        case disruption(FormattedAlert, iconName: String)
        case some(TripInstantDisplay)
    }

    var body: some View {
        predictionView
            .frame(minWidth: 48, alignment: .trailing)
            .padding(.trailing, 4)
    }

    @ViewBuilder
    var predictionView: some View {
        switch prediction {
        case let .some(prediction):
            switch onEnum(of: prediction) {
            case let .overridden(overridden):
                Text(overridden.textWithLocale()).realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(min(1.0, maxTextAlpha))
            case .hidden, .skipped:
                // should have been filtered out already
                Text(verbatim: "")
            case .now:
                Text("Now", comment: "Label for a trip that's arriving right now")
                    .font(Typography.headlineBold)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(min(1.0, maxTextAlpha))
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .arrivingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.arrivingOther())
            case .boarding:
                Text("BRD", comment: "Shorthand for boarding")
                    .font(Typography.headlineBold)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(min(1.0, maxTextAlpha))
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .boardingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.boardingOther())
            case .arriving:
                Text("ARR", comment: "Shorthand for arriving")
                    .font(Typography.headlineBold)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(min(1.0, maxTextAlpha))
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters
                        .arrivingFirst(vehicleText: routeType?.typeText(isOnly: isOnly) ?? "")
                        : accessibilityFormatters.arrivingOther())
            case .approaching:
                PredictionText(minutes: 1)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(min(1.0, maxTextAlpha))
            case let .time(format):
                Text(Date(instant: format.predictionTime), style: .time)
                    .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(min(1.0, maxTextAlpha))
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.distantFutureFirst(
                            date: format.predictionTime.toNSDate(),
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters
                        .distantFutureOther(date: format.predictionTime.toNSDate()))
            case let .timeWithStatus(format):
                VStack(alignment: .trailing, spacing: 0) {
                    Text(Date(instant: format.predictionTime), style: .time)
                        .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                        .realtime(hideIndicator: hideRealtimeIndicators)
                        .opacity(min(1.0, maxTextAlpha))
                        .accessibilityLabel(isFirst
                            ? accessibilityFormatters.distantFutureFirst(
                                date: format.predictionTime.toNSDate(),
                                vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                            )
                            : accessibilityFormatters
                            .distantFutureOther(date: format.predictionTime.toNSDate()))
                    Text(format.status)
                        .font(Typography.footnoteSemibold)
                        .opacity(min(0.6, maxTextAlpha))
                        .multilineTextAlignment(.trailing)
                }
            case let .timeWithSchedule(format):
                VStack(alignment: .trailing, spacing: 0) {
                    Text(Date(instant: format.predictionTime), style: .time)
                        .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                        .realtime(hideIndicator: hideRealtimeIndicators)
                        .opacity(min(1.0, maxTextAlpha))
                        .accessibilityLabel(isFirst
                            ? accessibilityFormatters.distantFutureFirst(
                                date: format.predictionTime.toNSDate(),
                                vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                            )
                            : accessibilityFormatters
                            .distantFutureOther(date: format.predictionTime.toNSDate()))
                    Text(Date(instant: format.scheduledTime), style: .time)
                        .font(Typography.footnoteSemibold)
                        .opacity(min(0.6, maxTextAlpha))
                        .strikethrough()
                        .multilineTextAlignment(.trailing)
                }
            case let .minutes(format):
                PredictionText(minutes: format.minutes)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(min(1.0, maxTextAlpha))
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.predictionMinutesFirst(
                            minutes: format.minutes,
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters.predictionMinutesOther(minutes: format.minutes))
            case let .scheduleTime(format):
                Text(format.scheduledTime.toNSDate(), style: .time)
                    .opacity(min(0.6, maxTextAlpha))
                    .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.scheduleTimeFirst(
                            date: format.scheduledTime.toNSDate(),
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters.scheduleTimeOther(date: format.scheduledTime.toNSDate()))
            case let .scheduleMinutes(format):
                PredictionText(minutes: format.minutes)
                    .opacity(min(0.6, maxTextAlpha))
                    .accessibilityLabel(isFirst
                        ? accessibilityFormatters.scheduleMinutesFirst(
                            minutes: format.minutes,
                            vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                        )
                        : accessibilityFormatters.scheduleMinutesOther(minutes: format.minutes))
            case let .cancelled(format):
                HStack(spacing: Self.subjectSpacing) {
                    Text("Cancelled", comment: "The status label for a cancelled trip")
                        .font(Typography.footnote)
                        .opacity(min(0.6, maxTextAlpha))
                    Text(format.scheduledTime.toNSDate(), style: .time)
                        .font(Typography.footnoteSemibold)
                        .strikethrough()
                        .opacity(min(0.6, maxTextAlpha))
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(isFirst
                    ? accessibilityFormatters.cancelledFirst(
                        date: format.scheduledTime.toNSDate(),
                        vehicleText: routeType?.typeText(isOnly: isOnly) ?? ""
                    )
                    : accessibilityFormatters.cancelledOther(date: format.scheduledTime.toNSDate()))
            }
        case let .disruption(formattedAlert, iconName: iconName):
            DisruptionView(spec: formattedAlert.predictionReplacement, iconName: iconName, maxTextAlpha: maxTextAlpha)
        case let .noTrips(format):
            switch onEnum(of: format) {
            case .predictionsUnavailable:
                Text(
                    "Predictions unavailable",
                    comment: "The status label when no predictions exist for a route and direction"
                ).font(Typography.footnote)
            case .serviceEndedToday:
                Text(
                    "Service ended",
                    comment: "The status label for a route and direction when service was running earlier, but no more trips are running today"
                ).font(Typography.footnote)
            case .noSchedulesToday:
                Text(
                    "No service today",
                    comment: "The status label for a route when no service is running for the entire service day"
                ).font(Typography.footnote)
            }
        case .loading:
            ProgressView()
        }
    }
}

func makeTimeFormatter() -> DateFormatter {
    let formatter = DateFormatter()
    formatter.dateStyle = .none
    formatter.timeStyle = .short
    return formatter
}

struct DisruptionView: View {
    let spec: FormattedAlert.PredictionReplacement
    let iconName: String
    let maxTextAlpha: Double

    @ScaledMetric private var iconSize: CGFloat = 20

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 4) {
                fullText
                    .lineLimit(1)
                fullImage
            }
            VStack(alignment: .trailing, spacing: 4) {
                fullText
                    .lineLimit(1)
                fullImage
            }
            HStack(spacing: 4) {
                fullText
                fullImage
            }
        }
    }

    var rawText: Text {
        if let accessibilityLabel = spec.accessibilityLabel {
            Text(spec.text).accessibilityLabel(accessibilityLabel)
        } else {
            Text(spec.text)
        }
    }

    var fullText: some View {
        rawText
            .font(Typography.footnoteSemibold)
            .opacity(min(0.6, maxTextAlpha))
    }

    var fullImage: some View {
        Image(iconName)
            .resizable()
            .scaledToFill()
            .foregroundStyle(Color.deemphasized)
            .frame(width: iconSize, height: iconSize)
    }
}

struct UpcomingTripView_Previews: PreviewProvider {
    static let route = MapStopRoute.orange

    static func disruption(_ effect: Shared.Alert.Effect) -> UpcomingTripView.State {
        let alert = ObjectCollectionBuilder.Single.shared.alert { $0.effect = effect }
        let format = UpcomingFormat.Disruption(alert: alert, mapStopRoute: route)
        return .disruption(.init(alert: alert), iconName: format.iconName)
    }

    static var previews: some View {
        VStack(alignment: .trailing) {
            UpcomingTripView(prediction: disruption(.suspension), routeType: .heavyRail)
            UpcomingTripView(prediction: disruption(.stopClosure), routeType: .heavyRail)
            UpcomingTripView(prediction: disruption(.stationClosure), routeType: .heavyRail)
            UpcomingTripView(prediction: disruption(.dockClosure), routeType: .heavyRail)
            UpcomingTripView(prediction: disruption(.detour), routeType: .heavyRail)
            UpcomingTripView(prediction: disruption(.snowRoute), routeType: .heavyRail)
            UpcomingTripView(prediction: disruption(.shuttle), routeType: .heavyRail)
        }
        .padding(8)
        .frame(maxWidth: 150)
        .background(Color.fill3)
        .previewDisplayName("No Service")
    }
}
