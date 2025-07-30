//
//  ToastModifier.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct ToastModifier: ViewModifier {
    let vm: IToastViewModel
    @State private var state: ToastViewModel.State = ToastViewModel.StateHidden()
    @State private var workItem: DispatchWorkItem?
    @State private var toastState: ToastState?

    func body(content: Content) -> some View {
        content
            .overlay(
                toast()
                    .animation(.easeInOut(duration: 0.2), value: toastState)
            )
            .task {
                for await model in vm.models {
                    state = model
                }
            }
            .onChange(of: state) { _ in
                switch onEnum(of: state) {
                case .hidden:
                    toastState = nil
                case let .visible(state):
                    showToast(state: state.toast)
                }
            }
    }

    @ViewBuilder
    private func toast() -> some View {
        if let toastState {
            VStack {
                Spacer()
                ToastView(state: toastState, onDismiss: { dismissToast() })
                    .onAppear { announceToast(message: toastState.message) }
            }
        }
    }

    private func showToast(state: ToastState) {
        UIImpactFeedbackGenerator(style: .light)
            .impactOccurred()
        toastState = state

        let duration = state.duration.inMillis

        if let duration {
            workItem?.cancel()
            let task = DispatchWorkItem { dismissToast() }
            workItem = task
            DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(duration), execute: task)
        }
    }

    private func dismissToast() {
        vm.hideToast()
        workItem?.cancel()
        workItem = nil
    }

    private func announceToast(message: String) {
        if #available(iOS 17, *) {
            var toastAnnouncement = AttributedString(message)
            toastAnnouncement.accessibilitySpeechAnnouncementPriority = .high
            AccessibilityNotification.Announcement(toastAnnouncement).post()
        } else {
            UIAccessibility.post(
                notification: .layoutChanged,
                argument: message
            )
        }
    }
}

extension ToastViewModel.Duration {
    var inMillis: Int? {
        switch self {
        case .indefinite: nil
        case .short: 4000
        case .long: 10000
        }
    }
}

extension View {
    func toast(vm: IToastViewModel) -> some View {
        modifier(ToastModifier(vm: vm))
    }
}
