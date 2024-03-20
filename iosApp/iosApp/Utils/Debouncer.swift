//
//  Debouncer.swift
//  iosApp
//
//  Created by Simon, Emma on 3/19/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation

class Debouncer {
    private let delay: TimeInterval
    private var workItem: DispatchWorkItem?
    private let queue: DispatchQueue

    init(delay: TimeInterval, queue: DispatchQueue = DispatchQueue.main) {
        self.delay = delay
        self.queue = queue
    }

    func debounce(action: @escaping (() -> Void)) {
        workItem?.cancel()
        workItem = DispatchWorkItem { [weak self] in
            action()
            self?.workItem = nil
        }
        if let workItem {
            queue.asyncAfter(deadline: .now() + delay, execute: workItem)
        }
    }
}
