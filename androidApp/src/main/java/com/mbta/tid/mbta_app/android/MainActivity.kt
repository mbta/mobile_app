package com.mbta.tid.mbta_app.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessaging
import com.mbta.tid.mbta_app.android.analytics.AnalyticsProvider
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.android.util.fcmToken
import com.mbta.tid.mbta_app.initializeSentry
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.PushNotificationPayload
import com.mbta.tid.mbta_app.routes.DeepLinkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val deepLinkStateFlow: MutableStateFlow<DeepLinkState?> = MutableStateFlow(null)

    fun handleIntent(intent: Intent?) {
        val rawPushNotificationPayload = intent?.getStringExtra(PushNotificationPayload.launchKey)
        if (rawPushNotificationPayload != null) {
            val pushNotificationPayload: PushNotificationPayload =
                json.decodeFromString(rawPushNotificationPayload)
            val stillActive = pushNotificationPayload.isStillActive()
            AnalyticsProvider(Firebase.analytics)
                .notificationClicked(pushNotificationPayload, stillActive)
            deepLinkStateFlow.value = pushNotificationPayload.getDeepLinkState()
        } else {
            val deepLinkUri: Uri? = intent?.data?.takeIf { intent.action == Intent.ACTION_VIEW }
            deepLinkStateFlow.value =
                deepLinkUri?.let { DeepLinkState.from(it.toString()) } ?: DeepLinkState.None
        }
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
                    ContentView(deepLinkStateFlow.asStateFlow(), ::clearDeepLink)
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
