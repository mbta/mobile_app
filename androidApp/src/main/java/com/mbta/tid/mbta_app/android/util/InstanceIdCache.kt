package com.mbta.tid.mbta_app.android.util

import android.util.Log
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InstanceIdCache {
    companion object {
        val shared = InstanceIdCache()
    }

    private val _instanceId = MutableStateFlow<String?>(null)
    val instanceId = _instanceId.asStateFlow()

    init {
        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _instanceId.value = task.result
            } else {
                Log.e(
                    "InstanceIdCache",
                    "Failed to load Firebase Installation ID: ${task.exception}",
                )
            }
        }
    }
}
