package com.mbta.tid.mbta_app.viewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

public actual abstract class MoleculeScopeViewModel actual constructor() {
    internal actual val scope: CoroutineScope = MainScope()
}
