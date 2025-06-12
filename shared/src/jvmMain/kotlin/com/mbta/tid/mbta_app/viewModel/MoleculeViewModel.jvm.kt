package com.mbta.tid.mbta_app.viewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

actual abstract class MoleculeScopeViewModel actual constructor() {
    actual val scope: CoroutineScope = MainScope()
}
