//
//  VehicleMarkerView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-08-16.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct VehicleMarkerView: View {
    let route: Route
    let vehicle: Vehicle
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        ZStack {
            ZStack {
                Image(.vehicleHalo)
                Image(.vehiclePuck).foregroundStyle(route.uiColor)
            }
            .frame(width: 32, height: 32)
            .rotationEffect(.degrees(45))
            .rotationEffect(.degrees(vehicle.bearing?.doubleValue ?? 0))
            routeIcon(route)
                .foregroundStyle(Color(hex: route.textColor))
                .frame(height: 18)
        }
        .padding(10)
        .modifier(PulsingHaloModifier(isSelected: isSelected, routeColor: route.uiColor))
        .accessibilityHidden(true)
        .onTapGesture { onTap() }
    }

    struct PulsingHaloModifier: ViewModifier {
        let isSelected: Bool
        let routeColor: Color
        @State private var animationAtEnd = false

        func body(content: Content) -> some View {
            if isSelected {
                ZStack {
                    Circle().frame(width: 56).foregroundStyle(routeColor.opacity(0.33))
                    Circle().stroke(routeColor.opacity(animationAtEnd ? 0.1 : 0.5), lineWidth: 2)
                        .frame(width: animationAtEnd ? 56 : 0)
                        .onAppear {
                            withAnimation(.easeOut(duration: 2).delay(2).repeatForever(autoreverses: false)) {
                                animationAtEnd = true
                            }
                        }
                    content
                }
            } else {
                content
            }
        }
    }
}

#Preview {
    let objects = ObjectCollectionBuilder()
    let route = objects.route { route in
        route.color = "DA291C"
        route.textColor = "FFFFFF"
    }
    let vehicle = objects.vehicle { vehicle in
        vehicle.currentStatus = .inTransitTo
        vehicle.bearing = 225
    }
    return VehicleMarkerView(route: route, vehicle: vehicle, isSelected: true, onTap: {})
}
