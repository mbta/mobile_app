package com.mbta.tid.mbta_app.utils

public data class NavigationCallbacks(
    val onBack: (() -> Unit)?,
    val onClose: (() -> Unit)?,
    val backButtonPresentation: BackButtonPresentation,
) {
    public sealed class BackButtonPresentation {
        public data object Floating : BackButtonPresentation()

        public data object Header : BackButtonPresentation()
    }

    public companion object {
        public val empty: NavigationCallbacks =
            NavigationCallbacks(
                onBack = null,
                onClose = null,
                backButtonPresentation = BackButtonPresentation.Floating,
            )
    }
}
