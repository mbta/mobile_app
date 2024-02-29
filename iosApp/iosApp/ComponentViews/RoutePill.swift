//
//  RoutePill.swift
//  iosApp
//
//  Created by Simon, Emma on 2/15/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct RoutePill: View {
    let route: Route?
    let textColor: Color?
    let routeColor: Color?

    init(route: Route?) {
        self.route = route
        textColor = route?.textColor != nil ? Color(hex: route!.textColor) : nil
        routeColor = route?.color != nil ? Color(hex: route!.color) : nil
    }

    func getPillText() -> String {
        if route == nil {
            return ""
        }
        return switch route!.type {
        case .bus:
            route!.shortName
        default:
            route!.longName
        }
    }

    var body: some View {
        if route == nil {
            EmptyView()
        } else {
            Text(getPillText())
                .textCase(.uppercase)
                .font(.system(size: 17, weight: .bold))
                .frame(minWidth: 25)
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .lineLimit(1)
                .foregroundColor(textColor)
                .background(routeColor)
                .clipShape(Capsule())
        }
    }
}

struct RoutePill_Previews: PreviewProvider {
    static var previews: some View {
        List {
            HStack {
                RoutePill(route: Route(
                    id: "216",
                    type: .bus,
                    color: "FFC72C",
                    directionNames: ["Outbound", "Inbound"],
                    directionDestinations: ["Houghs Neck", "Quincy Center Station"],
                    longName: "Houghs Neck - Quincy Center Station via Germantown",
                    shortName: "216",
                    sortOrder: 52160,
                    textColor: "000000",
                    routePatterns: nil
                ))
                RoutePill(route: Route(
                    id: "1",
                    type: .bus,
                    color: "FFC72C",
                    directionNames: ["Outbound", "Inbound"],
                    directionDestinations: ["Harvard Square", "Nubian Station"],
                    longName: "Harvard Square - Nubian Station",
                    shortName: "1",
                    sortOrder: 50010,
                    textColor: "000000",
                    routePatterns: nil
                ))
                RoutePill(route: Route(
                    id: "627",
                    type: .bus,
                    color: "FFC72C",
                    directionNames: ["Outbound", "Inbound"],
                    directionDestinations: ["Bedford VA Hospital", "Alewife Station"],
                    longName: "Bedford VA Hospital - Alewife Station via Hanscom Airport",
                    shortName: "62/76",
                    sortOrder: 50621,
                    textColor: "000000",
                    routePatterns: nil
                ))
            }

            HStack {
                RoutePill(route: Route(
                    id: "Red",
                    type: .heavyRail,
                    color: "DA291C",
                    directionNames: ["South", "North"],
                    directionDestinations: ["Ashmont/Braintree", "Alewife"],
                    longName: "Red Line",
                    shortName: "",
                    sortOrder: 10010,
                    textColor: "FFFFFF",
                    routePatterns: nil
                ))
                RoutePill(route: Route(
                    id: "Blue",
                    type: .heavyRail,
                    color: "003DA5",
                    directionNames: ["West", "East"],
                    directionDestinations: ["Bowdoin", "Wonderland"],
                    longName: "Blue Line",
                    shortName: "",
                    sortOrder: 10040,
                    textColor: "FFFFFF",
                    routePatterns: nil
                ))
            }
            RoutePill(route: Route(
                id: "Green-C",
                type: .lightRail,
                color: "00843D",
                directionNames: ["West", "East"],
                directionDestinations: ["Cleveland Circle", "Government Center"],
                longName: "Green Line C",
                shortName: "C",
                sortOrder: 10033,
                textColor: "FFFFFF",
                routePatterns: nil
            ))
            RoutePill(route: Route(
                id: "CR-Middleborough",
                type: .commuterRail,
                color: "80276C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Middleborough/Lakeville", "South Station"],
                longName: "Middleborough/Lakeville Line",
                shortName: "",
                sortOrder: 20009,
                textColor: "FFFFFF",
                routePatterns: nil
            ))
            RoutePill(route: Route(
                id: "CR-Providence",
                type: .commuterRail,
                color: "80276C",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Stoughton or Wickford Junction", "South Station"],
                longName: "Providence/Stoughton Line",
                shortName: "",
                sortOrder: 20012,
                textColor: "FFFFFF",
                routePatterns: nil
            ))
            RoutePill(route: Route(
                id: "Boat-F1",
                type: .ferry,
                color: "008EAA",
                directionNames: ["Outbound", "Inbound"],
                directionDestinations: ["Hingham or Hull", "Long Wharf or Rowes Wharf"],
                longName: "Hingham/Hull Ferry",
                shortName: "",
                sortOrder: 30002,
                textColor: "FFFFFF",
                routePatterns: nil
            ))
            RoutePill(route: Route(
                id: "Shuttle-BroadwayKendall",
                type: .bus,
                color: "FFC72C",
                directionNames: ["South", "North"],
                directionDestinations: ["Ashmont/Braintree", "Alewife"],
                longName: "Kendall/MIT - Broadway via Downtown Crossing",
                shortName: "Red Line Shuttle",
                sortOrder: 61050,
                textColor: "000000",
                routePatterns: nil
            ))
            RoutePill(route: Route(
                id: "Shuttle-BrooklineHillsKenmore",
                type: .bus,
                color: "FFC72C",
                directionNames: ["West", "East"],
                directionDestinations: ["Riverside", "Union Square"],
                longName: "Brookline Hills - Kenmore",
                shortName: "Green Line D Shuttle",
                sortOrder: 61100,
                textColor: "000000",
                routePatterns: nil
            ))
        }.previewDisplayName("RoutePill")
    }
}
