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

    private static let subjectSpacing: CGFloat = 4
    @ScaledMetric private var iconSize: CGFloat = 16

    var baseOpacity: Double { min(1.0, maxTextAlpha) }
    var dimmedOpacity: Double { min(0.6, maxTextAlpha) }

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

    // swiftlint:disable:next function_body_length
    @ViewBuilder func tripInstantView(_ prediction: TripInstantDisplay) -> some View {
        let label = prediction.accessibilityLabel(
            isFirst: isFirst,
            vehicleType: routeType?.typeText(isOnly: isOnly) ?? ""
        )

        switch onEnum(of: prediction) {
        case let .overridden(format):
            Text(format.textWithLocale())
                .font(.footnoteSemibold)
                .opacity(dimmedOpacity)
                .lastTripPrefix(last: format.last)
                .realtime(hideIndicator: hideRealtimeIndicators)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case .hidden, .skipped:
            // should have been filtered out already
            Text(verbatim: "")
        case let .now(format):
            Text("Now", comment: "Label for a trip that's arriving right now")
                .font(Typography.headlineBold)
                .lastTripPrefix(last: format.last)
                .realtime(hideIndicator: hideRealtimeIndicators)
                .opacity(baseOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .boarding(format):
            Text("BRD", comment: "Shorthand for boarding")
                .font(Typography.headlineBold)
                .lastTripPrefix(last: format.last)
                .realtime(hideIndicator: hideRealtimeIndicators)
                .opacity(baseOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .arriving(format):
            Text("ARR", comment: "Shorthand for arriving")
                .font(Typography.headlineBold)
                .lastTripPrefix(last: format.last)
                .realtime(hideIndicator: hideRealtimeIndicators)
                .opacity(baseOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .approaching(format):
            PredictionText(minutes: 1)
                .lastTripPrefix(last: format.last)
                .realtime(hideIndicator: hideRealtimeIndicators)
                .opacity(baseOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .time(format):
            Text(format.predictionTime, style: .time)
                .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                .lastTripPrefix(last: format.last)
                .realtime(hideIndicator: hideRealtimeIndicators)
                .opacity(baseOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .timeWithStatus(format):
            VStack(alignment: .trailing, spacing: 0) {
                Text(format.predictionTime, style: .time)
                    .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                    .lastTripPrefix(last: format.last)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(baseOpacity)
                Text(format.status)
                    .font(Typography.footnoteSemibold)
                    .opacity(dimmedOpacity)
                    .multilineTextAlignment(.trailing)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
        case let .timeWithSchedule(format):
            VStack(alignment: .trailing, spacing: 0) {
                Text(format.predictionTime, style: .time)
                    .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                    .lastTripPrefix(last: format.last)
                    .realtime(hideIndicator: hideRealtimeIndicators)
                    .opacity(baseOpacity)
                Text(format.scheduledTime, style: .time)
                    .font(Typography.footnoteSemibold)
                    .opacity(dimmedOpacity)
                    .strikethrough()
                    .multilineTextAlignment(.trailing)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
        case let .minutes(format):
            PredictionText(minutes: format.minutes)
                .lastTripPrefix(last: format.last)
                .realtime(hideIndicator: hideRealtimeIndicators)
                .opacity(baseOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .scheduleTime(format):
            Text(format.scheduledTime, style: .time)
                .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                .lastTripPrefix(last: format.last, scheduleClock: true)
                .opacity(dimmedOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .scheduleTimeWithStatusColumn(format):
            VStack(alignment: .trailing, spacing: 0) {
                Text(format.scheduledTime, style: .time)
                    .lastTripPrefix(last: format.last, scheduleClock: true)
                    .opacity(dimmedOpacity)
                    .font(format.headline ? Typography.headlineSemibold : Typography.footnoteSemibold)
                Text(format.status)
                    .font(Typography.footnoteSemibold)
                    .opacity(dimmedOpacity)
                    .multilineTextAlignment(.trailing)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
        case let .scheduleTimeWithStatusRow(format):
            HStack(alignment: .center, spacing: 4) {
                Text(format.status)
                    .font(Typography.footnoteSemibold)
                    .opacity(dimmedOpacity)
                    .multilineTextAlignment(.trailing)
                Text(format.scheduledTime, style: .time)
                    .opacity(dimmedOpacity)
                    .font(Typography.footnoteSemibold)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
        case let .scheduleMinutes(format):
            PredictionText(minutes: format.minutes)
                .lastTripPrefix(last: format.last, scheduleClock: true)
                .opacity(dimmedOpacity)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
        case let .cancelled(format):
            HStack(spacing: Self.subjectSpacing) {
                Text("Cancelled", comment: "The status label for a cancelled trip")
                    .font(Typography.footnote)
                    .opacity(dimmedOpacity)
                Text(format.scheduledTime, style: .time)
                    .font(Typography.footnoteSemibold)
                    .strikethrough()
                    .opacity(dimmedOpacity)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
        }
    }

    @ViewBuilder
    var predictionView: some View {
        switch prediction {
        case let .some(prediction): tripInstantView(prediction)
        case let .disruption(formattedAlert, iconName: iconName):
            DisruptionView(spec: formattedAlert.predictionReplacement, iconName: iconName, maxTextAlpha: maxTextAlpha)
        case let .noTrips(format):
            switch onEnum(of: format) {
            case let .subwayEarlyMorning(format):
                HStack {
                    Image(.faClock).resizable().scaledToFit().frame(width: 12, height: 12)
                    Text(AttributedString.tryMarkdown(String(
                        format: NSLocalizedString(
                            "First **%@**",
                            comment: "Label for a first trip of the morning, e.g. “First 5:21 AM”"
                        ),
                        format.scheduledTime.formatted(date: .omitted, time: .shortened)
                    )))
                }
                .opacity(dimmedOpacity)
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

struct DisruptionView: View {
    let spec: FormattedAlert.PredictionReplacement
    let iconName: String
    let maxTextAlpha: Double

    @ScaledMetric private var iconSize: CGFloat = 20
    var dimmedOpacity: Double { min(0.6, maxTextAlpha) }

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
            .opacity(dimmedOpacity)
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

struct UpcomingTripViewLastTrip_Previews: PreviewProvider {
    static var previews: some View {
        VStack(alignment: .trailing) {
            UpcomingTripView(prediction: .some(.Now(last: true)))
            UpcomingTripView(prediction: .some(.Minutes(minutes: 5, last: true)))
            UpcomingTripView(prediction: .some(
                .TimeWithStatus(
                    predictionTime: .now().plus(minutes: 7),
                    status: "Now boarding",
                    last: true, headline: false
                )
            ))
            UpcomingTripView(prediction: .some(
                .TimeWithSchedule(
                    predictionTime: .now().plus(minutes: 7),
                    scheduledTime: .now().plus(minutes: 10),
                    last: true, headline: true
                )
            ))
            UpcomingTripView(prediction: .some(
                .Overridden(text: "Stopped 10 stops away", last: true)
            ))
            UpcomingTripView(prediction: .some(
                .ScheduleTime(
                    scheduledTime: .now().plus(minutes: 10),
                    last: true, headline: true,
                )
            ))
            UpcomingTripView(prediction: .some(
                .ScheduleTime(
                    scheduledTime: .now().plus(minutes: 10),
                    last: true, headline: false,
                )
            ))
            UpcomingTripView(prediction: .some(
                .ScheduleTimeWithStatusColumn(
                    scheduledTime: .now().plus(minutes: 10),
                    status: "All aboard",
                    last: true, headline: false,
                )
            ))
            UpcomingTripView(prediction: .some(
                .ScheduleMinutes(minutes: 18, last: true)
            ))
        }
        .padding(8)
        .frame(maxWidth: 200)
        .background(Color.fill3)
        .previewDisplayName("Last Trip")
    }
}
