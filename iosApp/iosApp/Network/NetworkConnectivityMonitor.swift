//
//  NetworkConnectivityMonitor.swift
//  iosApp
//
//  Created by esimon on 9/16/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Dispatch
import Network
import Reachability
import Shared

class NetworkConnectivityMonitor: INetworkConnectivityMonitor {
    let monitorQueue: DispatchQueue

    let monitor: NWPathMonitor
    let reachability: Reachability?

    init() {
        monitorQueue = DispatchQueue(label: "NetworkConnectivityMonitor.NWPathMonitor")
        monitor = NWPathMonitor()

        let reachabilityQueue = DispatchQueue(label: "NetworkConnectivityMonitor.Reachability")
        // Android hits a google endpoint to check connection status, also use it here for parity and uptime
        reachability = try? Reachability(hostname: "www.google.com", targetQueue: reachabilityQueue)
    }

    func registerListener(onNetworkAvailable: @escaping () -> Void, onNetworkLost: @escaping () -> Void) {
        monitor.pathUpdateHandler = { path in
            if path.status == .satisfied {
                onNetworkAvailable()
            } else {
                onNetworkLost()
            }
        }
        monitor.start(queue: monitorQueue)

        if let reachability {
            reachability.whenReachable = { _ in
                onNetworkAvailable()
            }
            reachability.whenUnreachable = { _ in
                onNetworkLost()
            }
            do {
                try reachability.startNotifier()
            } catch {
                Sentry.shared.captureMessage(message: "Failed to start Reachability notifier")
            }
        } else {
            Sentry.shared.captureMessage(message: "Failed to start Reachability notifier")
        }
    }
}
