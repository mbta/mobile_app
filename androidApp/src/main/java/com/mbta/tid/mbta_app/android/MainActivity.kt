package com.mbta.tid.mbta_app.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mbta.tid.mbta_app.initializeSentry
import io.sentry.kotlin.multiplatform.Sentry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSentry()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContentView()
                }
            }
        }
    }

    private fun initSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        val env = BuildConfig.SENTRY_ENVIRONMENT

        if (dsn != null && env != null && !BuildConfig.DEBUG) {
            initializeSentry(dsn, env)
            Log.i("MainActivity", "Sentry initialized")
            Sentry.captureMessage("KB TESTING MESSAGE")
        } else {
            Log.w("MainActivity", "skipping sentry initialization")
        }
    }
}
