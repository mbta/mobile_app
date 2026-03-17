//
//  ToastView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 7/23/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

typealias ToastState = ToastViewModel.Toast

let NOTIFICATIONS_BETA_TOAST_KEY = "notifications_beta_toast_message_key"

struct ToastView: View {
    @Environment(\.colorScheme) var colorScheme

    var state: ToastState
    var tabBarVisible: Bool
    var accessibilityLabel: Text?
    var onDismiss: () -> Void

    var body: some View {
        let text = if state.message == NOTIFICATIONS_BETA_TOAST_KEY {
            Text("\(Text("Get early access to Notifications").underline()) and provide feedback")
        } else {
            Text(AttributedString.tryMarkdown(state.message))
        }

        let resolvedLabel = if let accessibilityLabel { accessibilityLabel } else { text }

        let bodyAction: (() -> Void)? = switch onEnum(of: state.action) {
        case let .body(action): action.onAction
        case let .bodyWithClose(action): action.onAction
        default: nil
        }

        HStack {
            Group {
                text
                    .font(Typography.body)
                    .foregroundColor(Color.textContrast)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, 16)
                    .padding(.trailing, 8)
                    .accessibilityLabel(resolvedLabel)

                actionButton
            }
            .padding(.vertical, 16)
        }
        .onTapGesture {
            if let bodyAction {
                bodyAction()
            }
        }
        .frame(maxWidth: .infinity)
        .background(Color.contrast)
        .cornerRadius(8)
        .padding(.horizontal, 8)
        .padding(.bottom, tabBarVisible ? 64 : 32)
    }

    @ViewBuilder
    func closeButton(action: @escaping () -> Void) -> some View {
        ActionButton(
            kind: .dismiss,
            circleColor: Color.contrast,
            iconColor: Color.textContrast
        ) {
            action()
            onDismiss()
        }
        .overlay(Circle().stroke(Color.haloContrast, lineWidth: 2).frame(width: 34, height: 34))
        .padding(.trailing, 16)
    }

    @ViewBuilder
    var actionButton: some View {
        switch onEnum(of: state.action) {
        case let .close(action): closeButton(action: action.onClose)

        case let .bodyWithClose(action): closeButton(action: action.onClose)

        case let .custom(action): NavTextButton(
                string: action.actionLabel,
                backgroundColor: Color.contrast,
                textColor: Color.textContrast
            ) {
                action.onAction()
                onDismiss()
            }
            .withRoundedBorder(radius: 80, color: Color.haloContrast, width: 2)
            .padding(.trailing, 16)

        default: EmptyView()
        }
    }
}

#Preview {
    let textOnly = ToastState(
        message: "This is a text only toast",
        duration: ToastViewModel.Duration.indefinite,
        isTip: false,
        action: nil
    )
    let close = ToastState(
        message: "This is a toast with a close button",
        duration: ToastViewModel.Duration.indefinite,
        isTip: false,
        action: ToastViewModel.ToastActionClose(onClose: {})
    )
    let action = ToastState(
        message: "This is a toast with an action button",
        duration: ToastViewModel.Duration.indefinite,
        isTip: false,
        action: ToastViewModel.ToastActionCustom(actionLabel: "Action", onAction: {})
    )
    VStack {
        ToastView(state: textOnly, tabBarVisible: false, onDismiss: {})
        ToastView(state: close, tabBarVisible: false, onDismiss: {})
        ToastView(state: action, tabBarVisible: false, onDismiss: {})
    }
}
