package com.mbta.tid.mbta_app.android

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.messaging.FirebaseMessaging
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.android.util.fcmToken
import com.mbta.tid.mbta_app.initializeSentry
import com.mbta.tid.mbta_app.routes.DeepLinkState
import kotlin.toString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val deepLinkStateFlow: MutableStateFlow<DeepLinkState?> = MutableStateFlow(null)

    fun handleIntent(intent: Intent?) {
        val deepLinkUri: Uri? = intent?.data?.takeIf { intent.action == Intent.ACTION_VIEW }
        deepLinkStateFlow.update {
            deepLinkUri?.let { DeepLinkState.from(it.toString()) } ?: DeepLinkState.None
        }
    }

    fun clearDeepLink() = deepLinkStateFlow.update { null }

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
        enableEdgeToEdge(
            navigationBarStyle =
                if (isDarkModeOn()) {
                    SystemBarStyle.dark(scrim = getColor(R.color.fill2))
                } else {
                    SystemBarStyle.light(
                        scrim = getColor(R.color.fill2),
                        darkScrim = getColor(R.color.fill2),
                    )
                },
            statusBarStyle =
                if (isDarkModeOn()) {
                    SystemBarStyle.dark(scrim = Color(0x00000000).toArgb())
                } else {
                    SystemBarStyle.light(
                        scrim = Color(0x00000000).toArgb(),
                        darkScrim = Color(0x00000000).toArgb(),
                    )
                },
        )

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(LocalLocationClient provides fusedLocationClient) {
                        ContentView(deepLinkStateFlow.asStateFlow(), ::clearDeepLink)
                    }
                }
            }
        }
    }

    private fun isDarkModeOn(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkModeOn = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        return isDarkModeOn
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
