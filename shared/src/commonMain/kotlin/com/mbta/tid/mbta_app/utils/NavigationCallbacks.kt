package com.mbta.tid.mbta_app.utils

public data class NavigationCallbacks(
    val onBack: (() -> Unit)?,
    val onClose: (() -> Unit)?,
    val sheetBackState: SheetBackState,
) {
    public sealed class SheetBackState {
        public data object Hidden : SheetBackState()

        public data object Shown : SheetBackState()
    }

    public companion object {
        public val empty: NavigationCallbacks =
            NavigationCallbacks(
                onBack = null,
                onClose = null,
                sheetBackState = SheetBackState.Hidden,
            )
    }
}
