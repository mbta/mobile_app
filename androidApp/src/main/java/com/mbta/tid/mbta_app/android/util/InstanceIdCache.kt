package com.mbta.tid.mbta_app.android.util

import android.util.Log
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface IInstanceIdCache {
    val instanceId: StateFlow<String?>
}

class InstanceIdCache : IInstanceIdCache {
    private val _instanceId = MutableStateFlow<String?>(null)
    override val instanceId = _instanceId.asStateFlow()

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

class MockInstanceIdCache(initialInstanceId: String? = null) : IInstanceIdCache {
    override val instanceId = MutableStateFlow(initialInstanceId)
}
