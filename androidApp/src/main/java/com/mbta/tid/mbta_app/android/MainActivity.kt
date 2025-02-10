package com.mbta.tid.mbta_app.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mbta.tid.mbta_app.android.util.LocalActivity
import com.mbta.tid.mbta_app.android.util.LocalLocationClient
import com.mbta.tid.mbta_app.initializeSentry

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSentry()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(
                        LocalActivity provides this,
                        LocalLocationClient provides fusedLocationClient,
                    ) {
                        ContentView()
                    }
                }
            }
        }
    }

    private fun initSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        val env = BuildConfig.SENTRY_ENVIRONMENT

        if (dsn.isNotEmpty() && env.isNotEmpty() && !BuildConfig.DEBUG) {
            initializeSentry(dsn, env)
            Log.i("MainActivity", "Sentry initialized")
        } else {
            Log.w("MainActivity", "skipping sentry initialization")
        }
    }
}
