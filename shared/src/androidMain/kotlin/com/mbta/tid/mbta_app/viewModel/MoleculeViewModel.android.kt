package com.mbta.tid.mbta_app.viewModel

import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

public actual abstract class MoleculeScopeViewModel actual constructor() : ViewModel() {
    internal actual val scope by lazy {
        CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    }
}
