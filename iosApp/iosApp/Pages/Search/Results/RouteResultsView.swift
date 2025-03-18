//
//  RouteResultsView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct RouteResultsView: View {
    let routes: [RouteResult]

    var body: some View {
        VStack {
            Text("Routes")
                .font(Typography.subheadlineSemibold)
                .frame(maxWidth: .infinity, alignment: .leading)
            ResultContainer {
                VStack(spacing: .zero) {
                    ForEach(routes, id: \.id) { route in
                        Group {
                            if route.routeType == RouteType.bus {
                                Text(verbatim: "\(route.shortName) \(route.longName)")
                            } else {
                                Text(route.longName)
                            }
                        }
                        .font(Typography.bodySemibold)
                        .foregroundStyle(Color.text)
                        .multilineTextAlignment(.leading)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        if route != routes.last {
                            Divider().overlay(Color.halo)
                        }
                    }
                }
            }
        }
    }
}
