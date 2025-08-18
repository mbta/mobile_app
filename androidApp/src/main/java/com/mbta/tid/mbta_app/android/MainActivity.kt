package com.mbta.tid.mbta_app.android

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.initializeSentry
import com.mbta.tid.mbta_app.routes.DeepLinkState

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSentry()
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

        val deepLinkUri = intent.data?.takeIf { intent.action == Intent.ACTION_VIEW }
        val deepLinkState =
            deepLinkUri?.let { DeepLinkState.from(it.toString()) } ?: DeepLinkState.None

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CompositionLocalProvider(LocalLocationClient provides fusedLocationClient) {
                        ContentView(deepLinkState)
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
}
