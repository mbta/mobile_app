package com.mbta.tid.mbta_app.android.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Set in MBTAGoMessagingService or retrieved on app launch in MainActivity */
var fcmInstallationId: String? by mutableStateOf(null)
