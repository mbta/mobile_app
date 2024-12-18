//
//  ExplainerPage.swift
//  iosApp
//
//  Created by esimon on 12/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct Explainer {
    let type: ExplainerType
    let routeAccents: TripRouteAccents
}

enum ExplainerType: String {
    case noPrediction
}

struct ExplainerPage: View {
    let explainer: Explainer
    let onClose: () -> Void

    @ScaledMetric private var modeIconHeight: CGFloat = 24

    @ViewBuilder
    private var header: some View {
        HStack(alignment: .center, spacing: 6) {
            routeIcon(explainer.routeAccents.type)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .scaledToFit()
                .frame(maxHeight: modeIconHeight, alignment: .topLeading)

            Text("Details", comment: "Header on the general explainer details page").font(Typography.headline)
            Spacer()
            ActionButton(kind: .close) { onClose() }
        }
        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
        .padding(16)
        .foregroundStyle(explainer.routeAccents.textColor)
        .background(explainer.routeAccents.color)
    }

    @ViewBuilder
    private var explanationHeadline: some View {
        switch explainer.type {
        case .noPrediction: Text(
                "Prediction not available yet",
                comment: "Headline for an explanation of why no predictions are shown"
            )
        }
    }

    @ViewBuilder
    private var explanationText: some View {
        switch explainer.type {
        case .noPrediction: Text(
                "We don’t have live predictions for this trip yet, but they will appear closer to the scheduled time. If the trip is delayed or cancelled, we’ll let you know here."
            )
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            VStack(alignment: .leading, spacing: 24) {
                explanationHeadline.font(Typography.title2Bold)
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
}
