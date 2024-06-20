//
//  StopDetailsRoutePills.swift
//  iosApp
//
//  Created by Simon, Emma on 4/17/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct StopDetailsRoutePills: View {
    let servedRoutes: [(route: Route, line: Line?)]
    let tapRoutePill: (Route) -> Void
    @Binding var filter: StopDetailsFilter?

    var body: some View {
        HStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(servedRoutes, id: \.route.id) { route in
                            RoutePill(
                                route: route.route,
                                line: route.line,
                                type: .flex,
                                isActive: filter == nil || filter?.routeId == route.route.id
                            )
                            .frame(minWidth: 44, minHeight: 44, alignment: .center)
                            .onTapGesture { tapRoutePill(route.route) }
                        }
                    }
                    .padding(.horizontal, 15)
                }
                .task {
                    // no way to run code after appear, so instead, launch task before and wait
                    try? await Task.sleep(for: .milliseconds(10))
                    if let filteredRoute = filter?.routeId {
                        proxy.scrollTo(filteredRoute, anchor: .center)
                    }
                }
                .onChange(of: filter) { newFilter in
                    if let filteredRoute = newFilter?.routeId {
                        proxy.scrollTo(filteredRoute, anchor: .center)
                    }
                }
            }
            if filter != nil {
                Button(action: { filter = nil }) {
                    Text("All")
                        .foregroundStyle(Color.fill1)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 7)
                        .background(Color.contrast)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
                .padding(.trailing, 16)
            }
        }
    }
}
