package com.mbta.tid.mbta_app.viewModel

import app.cash.molecule.DisplayLinkClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

public actual abstract class MoleculeScopeViewModel actual constructor() {
    internal actual val scope = CoroutineScope(MainScope().coroutineContext + DisplayLinkClock)
}
