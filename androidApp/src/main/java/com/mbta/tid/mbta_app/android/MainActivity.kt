package com.mbta.tid.mbta_app.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.android.util.fcmToken
import com.mbta.tid.mbta_app.initializeSentry
import com.mbta.tid.mbta_app.routes.DeepLinkState
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val deepLinkStateFlow: MutableStateFlow<DeepLinkState?> = MutableStateFlow(null)

    fun handleIntent(intent: Intent?) {
        val deepLinkPath =
            if (intent?.hasExtra("deep_link_path") == true) {
                // TODO notification sound
                intent.getStringExtra("deep_link_path")
            } else if (intent?.action == Intent.ACTION_VIEW) {
                intent.dataString
            } else {
                null
            }
        deepLinkStateFlow.value = deepLinkPath?.let { DeepLinkState.from(it) }
    }

    fun clearDeepLink() {
        deepLinkStateFlow.value = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSentry()
        getFCMToken()
        handleIntent(intent)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        enableEdgeToEdge()

        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLocationClient provides fusedLocationClient) {
                    ContentView(deepLinkStateFlow, ::clearDeepLink)
                }
            }
        }
    }

    private fun initSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        val env = BuildConfig.SENTRY_ENVIRONMENT

        if (dsn.isNotEmpty() && !BuildConfig.DEBUG) {
            initializeSentry(dsn, env.ifEmpty { "debug" })
            Log.i("MainActivity", "Sentry initialized")
        } else {
            Log.w("MainActivity", "skipping sentry initialization")
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken = it }
    }
}
