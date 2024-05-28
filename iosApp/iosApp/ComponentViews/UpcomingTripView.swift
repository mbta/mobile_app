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
    var isFirst: Bool = false

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
                    .accessibilityLabel(accessibilityFormatters.boarding(isFirst: isFirst))
            case .arriving:
                Text("ARR").font(.headline).bold()
                    .accessibilityLabel(accessibilityFormatters.arriving(isFirst: isFirst))
            case .approaching:
                PredictionText(minutes: 1)
            case let .distantFuture(format):
                Text(Date(instant: format.predictionTime), style: .time)
                    .accessibilityLabel(accessibilityFormatters.distantFuture(date: format.predictionTime.toNSDate(),
                                                                              isFirst: isFirst))
                    .font(.footnote)
                    .fontWeight(.semibold)
            case let .schedule(schedule):
                HStack(spacing: Self.subjectSpacing) {
                    Text(schedule.scheduleTime.toNSDate(), style: .time)
                        .accessibilityLabel(accessibilityFormatters.scheduled(date: schedule.scheduleTime.toNSDate(),
                                                                              isFirst: isFirst))
                        .font(.footnote)
                        .fontWeight(.semibold)
                    Image(.faClock)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: iconSize, height: iconSize)
                        .padding(4)
                        .foregroundStyle(Color.deemphasized)
                }
            case let .minutes(format):
                PredictionText(minutes: format.minutes)
                    .accessibilityLabel(accessibilityFormatters.predictionMinutes(minutes: format.minutes,
                                                                                  isFirst: isFirst))
            }
        case let .noService(alertEffect):
            NoServiceView(effect: .from(alertEffect: alertEffect))
        case .none:
            Text("No Predictions")
        case .loading:
            ProgressView()
        }
    }
}

class UpcomingTripAccessibilityFormatters {
    private let timeFormatter: DateFormatter = makeTimeFormatter()
    public func boarding(isFirst: Bool) -> Text {
        isFirst ? Text("boarding now") : Text("and boarding now")
    }

    public func arriving(isFirst: Bool) -> Text {
        isFirst ? Text("arriving now") : Text("and arriving now")
    }

    public func distantFuture(date: Date, isFirst: Bool) -> Text {
        isFirst
            ? Text("arriving at \(timeFormatter.string(from: date))")
            : Text("and at \(timeFormatter.string(from: date))")
    }

    public func scheduled(date: Date, isFirst: Bool) -> Text {
        isFirst
            ? Text("arriving at \(timeFormatter.string(from: date)) scheduled")
            : Text("and at \(timeFormatter.string(from: date)) scheduled")
    }

    public func predictionMinutes(minutes: Int32, isFirst: Bool) -> Text {
        isFirst ? Text("arriving in \(minutes) minutes") : Text("and in \(minutes) minutes")
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
        case .shuttle: Text("Shuttle").accessibilityLabel(Text("Shuttle buses replace service"))
        case .stopClosed: Text("Stop Closed")
        case .suspension: Text("Suspension").accessibilityLabel(Text("Service suspended"))
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
            UpcomingTripView(prediction: .noService(.suspension))
            UpcomingTripView(prediction: .noService(.shuttle))
            UpcomingTripView(prediction: .noService(.stopClosure))
            UpcomingTripView(prediction: .noService(.detour))
        }
        .previewDisplayName("No Service")
    }
}
