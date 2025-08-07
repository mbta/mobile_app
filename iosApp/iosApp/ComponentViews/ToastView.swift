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

struct ToastView: View {
    @Environment(\.colorScheme) var colorScheme

    var state: ToastState
    var tabBarVisible: Bool
    var onDismiss: () -> Void

    var body: some View {
        HStack {
            Group {
                Text(AttributedString.tryMarkdown(state.message))
                    .font(Typography.body)
                    .foregroundColor(Color.textContrast)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.leading, 16)
                    .padding(.trailing, 8)

                actionButton
            }
            .padding(.vertical, 16)
        }
        .frame(maxWidth: .infinity)
        .background(Color.contrast)
        .cornerRadius(8)
        .padding(.horizontal, 8)
        .padding(.bottom, tabBarVisible ? 64 : 32)
    }

    @ViewBuilder
    var actionButton: some View {
        switch onEnum(of: state.action) {
        case let .close(closeAction): ActionButton(
                kind: .close,
                circleColor: Color.contrast,
                iconColor: Color.textContrast
            ) {
                closeAction.onClose()
                onDismiss()
            }
            .overlay(Circle().stroke(Color.haloContrast, lineWidth: 2).frame(width: 34, height: 34))
            .padding(.trailing, 16)

        case let .custom(customAction): NavTextButton(
                string: customAction.actionLabel,
                backgroundColor: Color.contrast,
                textColor: Color.textContrast
            ) {
                customAction.onAction()
                onDismiss()
            }
            .withRoundedBorder(radius: 80, color: Color.haloContrast, width: 2)
            .padding(.trailing, 16)

        case nil: EmptyView()
        }
    }
}

#Preview {
    let textOnly = ToastState(
        message: "This is a text only toast",
        duration: ToastViewModel.Duration.indefinite,
        action: nil
    )
    let close = ToastState(
        message: "This is a toast with a close button",
        duration: ToastViewModel.Duration.indefinite,
        action: ToastViewModel.ToastActionClose(onClose: {})
    )
    let action = ToastState(
        message: "This is a toast with an action button",
        duration: ToastViewModel.Duration.indefinite,
        action: ToastViewModel.ToastActionCustom(actionLabel: "Action", onAction: {})
    )
    VStack {
        ToastView(state: textOnly, tabBarVisible: false, onDismiss: {})
        ToastView(state: close, tabBarVisible: false, onDismiss: {})
        ToastView(state: action, tabBarVisible: false, onDismiss: {})
    }
}
