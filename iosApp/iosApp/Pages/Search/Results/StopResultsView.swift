//
//  StopResultsView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct StopResultsView: View {
    let stops: [SearchViewModel.StopResult]
    let handleStopTap: (String) -> Void

    var body: some View {
        ResultContainer {
            VStack(spacing: .zero) {
                ForEach(stops, id: \.id) { stop in
                    Button {
                        handleStopTap(stop.id)
                    } label: {
                        result(stop)
                    }
                    if stop != stops.last {
                        Divider().overlay(Color.halo)
                    }
                }
            }
        }
    }

    private func result(_ stop: SearchViewModel.StopResult) -> some View {
        HStack(spacing: 12) {
            Image(stop.isStation ? .tLogo : .mapStopCloseBUS)
                .frame(width: 32, height: 32)
            VStack {
                Text(stop.name)
                    .font(Typography.bodySemibold)
                    .foregroundStyle(Color.text)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 4) {
                        ForEach(stop.routePills, id: \.self) { routePill in
                            RoutePill(spec: routePill)
                        }
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    Text("serves \(stop.routePills.map { $0.contentDescription?.text ?? "" }.joined(separator: ","))")
                )
                .scrollBounceBehavior(.basedOnSize, axes: [.horizontal])
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Spacer()
        }
        .padding(12)
    }
}
