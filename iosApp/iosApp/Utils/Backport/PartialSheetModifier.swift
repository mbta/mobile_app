//
//  PartialSheetModifier.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 3/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

public extension View {
    /**
     Replaces the use of the presentationDetents & presentationBackgroundInteraction modifiers
     as they're only available in iOS 16+ for SwiftUI
     **/
    func partialSheetDetents(
        _ detents: Set<PartialSheetDetent>,
        largestUndimmedDetent: PartialSheetDetent
    ) -> some View {
        background(
            PartialSheetRepresentable(
                detents: detents,
                largestUndimmedDetent: largestUndimmedDetent
            )
        )
    }
}

private struct PartialSheetRepresentable: UIViewControllerRepresentable {
    let detents: Set<PartialSheetDetent>
    let largestUndimmedDetent: PartialSheetDetent?

    func makeUIViewController(context _: Context) -> Self.Controller {
        Controller(
            detents: detents,
            largestUndimmedDetent: largestUndimmedDetent
        )
    }

    func updateUIViewController(_ controller: Self.Controller, context _: Context) {
        controller.update(
            detents: detents,
            largestUndimmedDetent: largestUndimmedDetent
        )
    }
}

private extension PartialSheetRepresentable {
    final class Controller: UIViewController, UISheetPresentationControllerDelegate {
        var detents: Set<PartialSheetDetent>
        var largestUndimmedDetent: PartialSheetDetent?
        weak var localDelegate: UISheetPresentationControllerDelegate?

        init(
            detents: Set<PartialSheetDetent>,
            largestUndimmedDetent: PartialSheetDetent?
        ) {
            self.detents = detents
            self.largestUndimmedDetent = largestUndimmedDetent
            super.init(nibName: nil, bundle: nil)
        }

        @available(*, unavailable)
        required init?(coder _: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }

        override func willMove(toParent parent: UIViewController?) {
            super.willMove(toParent: parent)
            if let controller = parent?.sheetPresentationController,
               controller.delegate !== self,
               localDelegate == nil {
                localDelegate = controller.delegate
                controller.delegate = self
            }
            update(detents: detents, largestUndimmedDetent: largestUndimmedDetent)
        }

        override func willTransition(
            to newCollection: UITraitCollection,
            with coordinator: UIViewControllerTransitionCoordinator
        ) {
            super.willTransition(to: newCollection, with: coordinator)
            update(detents: detents, largestUndimmedDetent: largestUndimmedDetent)
        }

        override func responds(to aSelector: Selector!) -> Bool {
            if super.responds(to: aSelector) { return true }
            if localDelegate?.responds(to: aSelector) == true { return true }
            return false
        }

        override func forwardingTarget(for aSelector: Selector!) -> Any? {
            if super.responds(to: aSelector) { return self }
            return localDelegate
        }

        func update(
            detents: Set<PartialSheetDetent>,
            largestUndimmedDetent: PartialSheetDetent?
        ) {
            self.detents = detents

            guard let controller = parent?.sheetPresentationController else { return }

            controller.animateChanges {
                controller.detents = detents.map(\.uiKitDetent)
                controller.prefersScrollingExpandsWhenScrolledToEdge = true

                if let largestUndimmedDetent {
                    controller.largestUndimmedDetentIdentifier = .init(largestUndimmedDetent.rawValue)
                }
            }
        }
    }
}
