//
//  ExplainerPage.swift
//  iosApp
//
//  Created by esimon on 12/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

public struct Explainer {
    let type: ExplainerType
    let routeAccents: TripRouteAccents
}

enum ExplainerType: String {
    case finishingAnotherTrip
    case noPrediction
    case noVehicle
}

struct ExplainerPage: View {
    let explainer: Explainer
    let onClose: () -> Void

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            VStack(alignment: .leading, spacing: 24) {
                explanationHeadline
                    .font(Typography.title2Bold)
                    .accessibilityHeading(.h2)
                    .accessibilityAddTraits(.isHeader)
                explanationImage
                explanationText.font(Typography.body)
                Spacer()
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.top, 24)
            .padding(.bottom, 40)
        }
        .background(Color.fill2)
    }

    @ViewBuilder private var header: some View {
        HStack(alignment: .center, spacing: 6) {
            routeIcon(explainer.routeAccents.type)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .scaledToFit()
                .frame(maxHeight: modeIconHeight, alignment: .topLeading)
                .accessibilityHidden(true)

            Text("Details", comment: "Header on the general explainer details page")
                .font(Typography.headline)
                .accessibilityHeading(.h1)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            ActionButton(kind: .close) { onClose() }
        }
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        .padding(16)
        .foregroundStyle(explainer.routeAccents.textColor)
        .background(explainer.routeAccents.color)
    }

    @ViewBuilder private var explanationHeadline: some View {
        switch explainer.type {
        case .finishingAnotherTrip: Text(
                "Finishing another trip",
                comment: "Headline for a page explaining that the vehicle shown is finishing a different trip"
            )
        case .noPrediction: Text(
                "Prediction not available yet",
                comment: "Headline for an explanation of why no predictions are shown"
            )
        case .noVehicle: Text(localizedNoVehicleHeading)
        }
    }

    @ViewBuilder private var explanationImage: some View {
        switch explainer.type {
        case .finishingAnotherTrip: ZStack(alignment: .center) {
                Image(.turnaroundShape)
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(explainer.routeAccents.color)
                turnaroundModeImage
                    .resizable()
                    .scaledToFit()
                    .frame(maxWidth: .infinity)
                    .foregroundStyle(explainer.routeAccents.textColor)
            }.accessibilityHidden(true)
        default: EmptyView()
        }
    }

    @ViewBuilder private var explanationText: some View {
        switch explainer.type {
        case .finishingAnotherTrip: Text(localizedFinishingAnotherTripDescription)
        case .noPrediction: Text(
                "We don’t have live predictions for this trip yet, but they will appear closer to the scheduled time. If the trip is delayed or cancelled, we’ll let you know here."
            )
        case .noVehicle: Text(localizedNoVehicleDescription)
        }
    }

    // These string properties consistently capitalize the first character of the localized string,
    // since it can be interpolated with a lowercase "bus" or "train" string.

    private var localizedFinishingAnotherTripDescription: String {
        capitalizeFirst(String(format: NSLocalizedString(
            "The %@ assigned to this route is currently serving another trip. We’ll show it on the route once it starts this trip.",
            comment: """
            Description on an explainer page about missing vehicle location,
            the interpolated value can be either "bus" or "train"
            """
        ), modeText))
    }

    private var localizedNoVehicleHeading: String {
        capitalizeFirst(String(format: NSLocalizedString(
            "%@ location not available yet",
            comment: """
            Headline on an explainer page about missing vehicle location,
            the interpolated value can be either "bus" or "train",
            ex "[Bus] location not available yet"
            """
        ), modeText))
    }

    private var localizedNoVehicleDescription: String {
        capitalizeFirst(String(format: NSLocalizedString(
            "The %@ location might not be available in advance if a vehicle hasn’t been assigned yet. Once the driver starts the trip, we’ll start showing the live location.",
            comment: """
            Description on an explainer page about missing vehicle location,
            the interpolated value can be either "bus" or "train"
            """
        ), modeText))
    }

    private var modeText: String { explainer.routeAccents.type.typeText(isOnly: true) }

    private var turnaroundModeImage: Image {
        switch explainer.routeAccents.type {
        case .bus: Image(.turnaroundIconBus)
        case .commuterRail: Image(.turnaroundIconCommuter)
        case .ferry: Image(.turnaroundIconFerry)
        case .heavyRail, .lightRail: Image(.turnaroundIconSubway)
        }
    }

    func capitalizeFirst(_ rawString: String) -> String {
        rawString.prefix(1).capitalized + rawString.dropFirst()
    }
}
