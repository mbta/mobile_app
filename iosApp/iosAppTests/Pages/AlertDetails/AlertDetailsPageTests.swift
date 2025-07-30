//
//  AlertDetailsPageTests.swift
//  iosAppTests
//
//  Created by Simon, Emma on 8/12/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
@testable import iosApp
import Shared
import SwiftUI
import ViewInspector
import XCTest

final class AlertDetailsPageTests: XCTestCase {
    @MainActor func testAlertDetailsPageParentStopResolution() throws {
        let objects = ObjectCollectionBuilder()

        let stop1 = objects.stop { stop in
            stop.name = "Stop 1"
            stop.childStopIds = ["stop1a", "stop1b"]
        }
        let stop1a = objects.stop { stop in
            stop.id = "stop1a"
            stop.name = "Stop 1a"
            stop.parentStationId = stop1.id
        }
        let stop1b = objects.stop { stop in
            stop.id = "stop1b"
            stop.name = "Stop 1b"
            stop.parentStationId = stop1.id
        }
        let stop2 = objects.stop { stop in
            stop.name = "Stop 2"
            stop.childStopIds = ["stop2a"]
        }
        let stop2a = objects.stop { stop in
            stop.id = "stop2a"
            stop.name = "Stop 2a"
            stop.parentStationId = stop2.id
        }
        let stop3 = objects.stop { stop in
            stop.name = "Stop 3"
        }

        let route = objects.route { route in
            route.longName = "Orange Line"
        }

        let now = EasternTimeInstant.now()

        let alert = objects.alert { alert in
            alert.activePeriod(start: now.minus(seconds: 5), end: now.plus(seconds: 5))
            alert.description_ = "Long description"
            alert.cause = .fire
            alert.effect = .stopClosure
            alert.effectName = "Closure"
            alert.header = "Alert header"
            alert.informedEntity(
                activities: [.board, .exit, .ride],
                directionId: nil, facility: nil,
                route: route.id, routeType: nil,
                stop: stop1.id, trip: nil
            )
            alert.informedEntity(
                activities: [.board, .exit, .ride],
                directionId: 0, facility: nil,
                route: route.id, routeType: nil,
                stop: stop1a.id, trip: nil
            )
            alert.informedEntity(
                activities: [.board, .exit, .ride],
                directionId: 1, facility: nil,
                route: route.id, routeType: nil,
                stop: stop1b.id, trip: nil
            )
            alert.informedEntity(
                activities: [.board, .exit, .ride],
                directionId: nil, facility: nil,
                route: route.id, routeType: nil,
                stop: stop2a.id, trip: nil
            )
            alert.informedEntity(
                activities: [.board, .exit, .ride],
                directionId: nil, facility: nil,
                route: route.id, routeType: nil,
                stop: stop3.id, trip: nil
            )
        }

        let nearbyVM: NearbyViewModel = .init()
        nearbyVM.alerts = .init(alerts: [alert.id: alert])

        let globalDataLoaded = PassthroughSubject<Void, Never>()

        let sut = AlertDetailsPage(
            alertId: alert.id,
            line: nil,
            routes: [route],
            nearbyVM: nearbyVM,
            globalRepository: MockGlobalRepository(
                response: .init(objects: objects, patternIdsByStop: [:]),
                onGet: { globalDataLoaded.send() }
            )
        )

        let exp = sut.inspection.inspect(onReceive: globalDataLoaded, after: 1) { view in
            XCTAssertNotNil(try view.find(text: "Orange Line Stop Closure"))
            XCTAssertNotNil(try view.find(text: "Fire"))
            XCTAssertNotNil(try view.find(text: "3 affected stops"))
            XCTAssertNotNil(try view.find(text: "Stop 1"))
            XCTAssertNotNil(try view.find(text: "Stop 2"))
            XCTAssertNotNil(try view.find(text: "Stop 3"))
            XCTAssertNil(try? view.find(text: "Stop 1a"))
            XCTAssertNil(try? view.find(text: "Stop 1b"))
            XCTAssertNil(try? view.find(text: "Stop 2a"))
        }
        ViewHosting.host(view: sut)
        wait(for: [exp], timeout: 5)
    }
}
