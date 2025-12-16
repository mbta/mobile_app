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
    let vehicle: Vehicle
    let routeAccents: TripRouteAccents
    let isSelected: Bool
    let onTap: () -> Void
    let enlargeIfDecorated: Bool

    var body: some View {
        ZStack {
            ZStack {
                Image(.vehicleHalo)
                if vehicle.decoration == .pride {
                    VehiclePuckShape(bearing: vehicle.bearing?.doubleValue ?? 0)
                        .frame(width: 28, height: 28)
                        .rotationEffect(.degrees(360 - 45))
                        .rotationEffect(.degrees(360 - (vehicle.bearing?.doubleValue ?? 0)))
                        .foregroundStyle(Gradient(stops: [
                            .init(color: .init(hex: "7BD1EC"), location: 0),
                            .init(color: .init(hex: "7BD1EC"), location: 0.1),
                            .init(color: .init(hex: "F7AFC5"), location: 0.1),
                            .init(color: .init(hex: "F7AFC5"), location: 0.2),
                            .init(color: .init(hex: "623A16"), location: 0.2),
                            .init(color: .init(hex: "623A16"), location: 0.3),
                            .init(color: .init(hex: "000000"), location: 0.3),
                            .init(color: .init(hex: "000000"), location: 0.4),
                            .init(color: .init(hex: "DA291C"), location: 0.4),
                            .init(color: .init(hex: "DA291C"), location: 0.5),
                            .init(color: .init(hex: "ED8B00"), location: 0.5),
                            .init(color: .init(hex: "ED8B00"), location: 0.6),
                            .init(color: .init(hex: "FFC72C"), location: 0.6),
                            .init(color: .init(hex: "FFC72C"), location: 0.7),
                            .init(color: .init(hex: "00843D"), location: 0.7),
                            .init(color: .init(hex: "00843D"), location: 0.8),
                            .init(color: .init(hex: "003DA5"), location: 0.8),
                            .init(color: .init(hex: "003DA5"), location: 0.9),
                            .init(color: .init(hex: "80276C"), location: 0.9),
                            .init(color: .init(hex: "80276C"), location: 1),
                        ]))
                } else {
                    Image(.vehiclePuck).foregroundStyle(routeAccents.color)
                }
            }
            .frame(width: 32, height: 32)
            .rotationEffect(.degrees(45))
            .rotationEffect(.degrees(vehicle.bearing?.doubleValue ?? 0))
            vehicleIcon
                .frame(height: 18)
        }
        .padding(10)
        .modifier(PulsingHaloModifier(isSelected: isSelected, routeColor: routeAccents.color))
        .scaleEffect(vehicle.decoration != nil && enlargeIfDecorated ? 1.5 : 1)
        .accessibilityHidden(true)
        .onTapGesture { onTap() }
    }

    @ViewBuilder var vehicleIcon: some View {
        if vehicle.decoration == .googlyEyes, routeAccents.type == .commuterRail {
            Image(.vehicleCrEyes)
        } else if vehicle.decoration == .googlyEyes, routeAccents.type.isSubway() {
            Image(.vehicleSubwayEyes)
        } else if vehicle.decoration == .winterHoliday, routeAccents.type.isSubway() {
            Image(.vehicleSubwayWinterHoliday)
        } else if vehicle.decoration == .pride {
            routeIcon(routeAccents.type).foregroundStyle(.white)
        } else {
            routeIcon(routeAccents.type).foregroundStyle(routeAccents.textColor)
        }
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

    struct VehiclePuckShape: SwiftUI.Shape {
        let bearing: Double

        func path(in rect: CGRect) -> SwiftUI.Path {
            func p(_ origX: CGFloat, _ origY: CGFloat) -> CGPoint {
                .init(x: origX * rect.width / 28 + rect.minX, y: origY * rect.height / 28 + rect.minY)
            }
            return SwiftUI.Path {
                $0.move(to: p(0, 14))
                $0.addCurve(to: p(0, 1), control1: p(0, 11.09), control2: p(0, 3.69))
                $0.addCurve(to: p(1, 0), control1: p(0, 0.45), control2: p(0.44, 0))
                $0.addCurve(to: p(14, 0), control1: p(3.6, 0), control2: p(10.66, 0))
                $0.addCurve(to: p(28, 14), control1: p(21.73, 0), control2: p(28, 6.27))
                $0.addCurve(to: p(14, 28), control1: p(28, 21.73), control2: p(21.73, 28))
                $0.addCurve(to: p(0, 14), control1: p(6.27, 28), control2: p(0, 21.73))
                $0.closeSubpath()
            }.applying(.init(translationX: 14, y: 14).rotated(by: (bearing + 45).toRadians()).translatedBy(
                x: -14,
                y: -14
            ))
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
        vehicle.decoration = .pride
    }
    return VehicleMarkerView(
        vehicle: vehicle,
        routeAccents: .init(route: route),
        isSelected: true,
        onTap: {},
        enlargeIfDecorated: true
    )
}

struct DecorationPreview: PreviewProvider {
    struct Wrapper: View {
        let objects: ObjectCollectionBuilder
        let routeAccents: [TripRouteAccents]

        init() {
            objects = TestData.clone(namespace: "")
            objects.route {
                $0.color = "003DA5"
                $0.type = .heavyRail
                $0.textColor = "FFFFFF"
                $0.sortOrder = 10040
            }
            objects.route {
                $0.color = "008EAA"
                $0.type = .ferry
                $0.textColor = "FFFFFF"
                $0.sortOrder = 30002
            }
            let routes = objects.routes.allValues.compactMap { $0 as? Route }
                .sorted(by: { $0.sortOrder < $1.sortOrder })
            routeAccents = routes.map { TripRouteAccents(route: $0) }.removingDuplicates()
        }

        var body: some View {
            VStack(spacing: 16) {
                demoRow(.pride)
                demoRow(.winterHoliday, modes: [.lightRail, .heavyRail])
                demoRow(.googlyEyes, modes: [.lightRail, .heavyRail, .commuterRail])
            }
            .padding(16)
            .background(Color.fill1)
        }

        @ViewBuilder func demoRow(
            _ decoration: Vehicle.Decoration,
            modes: Set<RouteType> = Set(RouteType.allCases)
        ) -> some View {
            let routeAccents = routeAccents.filter { modes.contains($0.type) }
            Grid(horizontalSpacing: 0, verticalSpacing: 8) {
                ForEach(0 ..< Int(ceil(Double(routeAccents.count) / 4.0)), id: \.self) { indexChunk in
                    GridRow {
                        ForEach(indexChunk * 4 ..< min(routeAccents.count, (indexChunk + 1) * 4),
                                id: \.self) { routeIndex in
                            let routeAccents = routeAccents[routeIndex]
                            let vehicle = objects.vehicle {
                                $0.currentStatus = .inTransitTo
                                $0.decoration = decoration
                                $0.bearing = Double(45 * routeIndex)
                            }
                            VehicleMarkerView(
                                vehicle: vehicle,
                                routeAccents: routeAccents,
                                isSelected: false,
                                onTap: {},
                                enlargeIfDecorated: true
                            )
                        }
                    }
                }
            }
        }
    }

    static var previews: some View {
        Wrapper()
    }
}
