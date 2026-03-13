package com.mbta.tid.mbta_app.android.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mbta.tid.mbta_app.android.MainActivity
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.SheetHeader
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.key
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.WidgetTripConfig
import com.mbta.tid.mbta_app.model.response.ApiResult
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

class WidgetConfigActivity : ComponentActivity() {

    private val widgetPreferences: WidgetPreferences by lazy {
        WidgetPreferences(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var appWidgetId =
            intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetId = widgetPreferences.getAndClearPendingConfigWidgetId()
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    widgetPreferences = widgetPreferences,
                    onComplete = {
                        val resultIntent =
                            Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    widgetPreferences: WidgetPreferences,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val globalRepository: IGlobalRepository = koinInject()
    var globalResponse by remember { mutableStateOf<GlobalResponse?>(null) }
    var loadingTimeout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) { globalRepository.getGlobalData() }
        when (result) {
            is ApiResult.Ok -> globalResponse = result.data
            is ApiResult.Error -> {}
        }
    }

    LaunchedEffect(globalResponse) {
        if (globalResponse == null) {
            delay(8000)
            loadingTimeout = true
        } else {
            loadingTimeout = false
        }
    }

    var fromStop by remember { mutableStateOf<Stop?>(null) }
    var toStop by remember { mutableStateOf<Stop?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val selectableStops =
        remember(globalResponse, searchQuery, fromStop) {
            globalResponse?.let { global ->
                val query = searchQuery.trim().lowercase()
                val fromStopVal = fromStop
                val stops =
                    if (fromStopVal == null) {
                        global.getParentStopsForSelection()
                    } else {
                        global.getReachableDestinationStopsFrom(fromStopVal.id)
                    }
                if (query.isEmpty()) {
                    stops
                } else {
                    stops.filter { it.name.lowercase().contains(query) }
                }
            } ?: emptyList()
        }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        SheetHeader(
            title = stringResource(R.string.widget_configure_title),
            closeText = stringResource(R.string.cancel),
            navCallbacks =
                NavigationCallbacks(
                    onBack =
                        when {
                            toStop != null -> {
                                { toStop = null }
                            }
                            fromStop != null -> {
                                { fromStop = null }
                            }
                            else -> null
                        },
                    onClose = onCancel,
                    backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                ),
            buttonColors = ButtonDefaults.key(),
        )

        LazyColumn(
            modifier =
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (fromStop != null) {
                item(key = "from_chip") {
                    WidgetStopChip(
                        label = stringResource(R.string.widget_from),
                        stopName = fromStop!!.name,
                        onClear = {
                            fromStop = null
                            toStop = null
                        },
                    )
                }
            }
            if (toStop != null) {
                item(key = "to_chip") {
                    WidgetStopChip(
                        label = stringResource(R.string.widget_to),
                        stopName = toStop!!.name,
                        onClear = { toStop = null },
                    )
                }
            }

            if (fromStop == null || toStop == null) {
                item(key = "search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = {
                            Text(
                                stringResource(
                                    if (fromStop == null) R.string.widget_select_from_stop
                                    else R.string.widget_select_to_stop
                                )
                            )
                        },
                        singleLine = true,
                    )
                }

                when {
                    globalResponse == null && loadingTimeout -> {
                        item(key = "loading_timeout") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.widget_loading_timeout),
                                    style = Typography.body,
                                )
                                Button(
                                    onClick = {
                                        context.startActivity(
                                            Intent(context, MainActivity::class.java)
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                        onCancel()
                                    }
                                ) {
                                    Text(stringResource(R.string.widget_open_app))
                                }
                            }
                        }
                    }
                    globalResponse == null -> {
                        item(key = "loading") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.padding(16.dp))
                                Text(
                                    text = stringResource(R.string.loading),
                                    style = Typography.body,
                                )
                            }
                        }
                    }
                    else -> {
                        val global = globalResponse!!
                        item(key = "section_header") {
                            Text(
                                text =
                                    stringResource(
                                        if (fromStop == null) R.string.widget_selecting_from
                                        else R.string.widget_selecting_to
                                    ),
                                style = Typography.bodySemibold,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                        items(selectableStops, key = { it.id }) { stop ->
                            val resolved = stop.resolveParent(global)
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            if (fromStop == null) {
                                                fromStop = resolved
                                                searchQuery = ""
                                            } else {
                                                if (resolved.id == fromStop!!.id) {
                                                    Toast.makeText(
                                                            context,
                                                            context.getString(
                                                                R.string
                                                                    .widget_select_different_stops
                                                            ),
                                                            Toast.LENGTH_SHORT,
                                                        )
                                                        .show()
                                                } else {
                                                    toStop = resolved
                                                    searchQuery = ""

                                                    val fromResolved =
                                                        global
                                                            .getStop(fromStop!!.id)
                                                            ?.resolveParent(global)
                                                    val toResolved =
                                                        global
                                                            .getStop(toStop!!.id)
                                                            ?.resolveParent(global)
                                                    if (
                                                        fromResolved != null && toResolved != null
                                                    ) {
                                                        val config =
                                                            WidgetTripConfig(
                                                                fromStopId = fromResolved.id,
                                                                toStopId = toResolved.id,
                                                                fromLabel = fromResolved.name,
                                                                toLabel = toResolved.name,
                                                            )
                                                        coroutineScope.launch {
                                                            try {
                                                                withContext(Dispatchers.IO) {
                                                                    widgetPreferences.setConfig(
                                                                        appWidgetId,
                                                                        config,
                                                                    )
                                                                }
                                                                try {
                                                                    val glanceManager =
                                                                        GlanceAppWidgetManager(
                                                                            context
                                                                                .applicationContext
                                                                        )
                                                                    val glanceId =
                                                                        glanceManager.getGlanceIdBy(
                                                                            appWidgetId
                                                                        )
                                                                    MBTATripWidget()
                                                                        .update(
                                                                            context
                                                                                .applicationContext,
                                                                            glanceId,
                                                                        )
                                                                } catch (e: Exception) {}
                                                                delay(150)
                                                                onComplete()
                                                                WorkManager.getInstance(context)
                                                                    .enqueueUniqueWork(
                                                                        "WidgetConfigUpdate",
                                                                        ExistingWorkPolicy.REPLACE,
                                                                        OneTimeWorkRequestBuilder<
                                                                                WidgetUpdateWorker
                                                                            >()
                                                                            .setInputData(
                                                                                workDataOf(
                                                                                    WidgetUpdateWorker
                                                                                        .KEY_APP_WIDGET_IDS to
                                                                                        intArrayOf(
                                                                                            appWidgetId
                                                                                        )
                                                                                )
                                                                            )
                                                                            .setInitialDelay(
                                                                                800,
                                                                                TimeUnit
                                                                                    .MILLISECONDS,
                                                                            )
                                                                            .build(),
                                                                    )
                                                            } catch (e: Exception) {
                                                                android.util.Log.e(
                                                                    "WidgetConfig",
                                                                    "Failed to save widget config",
                                                                    e,
                                                                )
                                                                Toast.makeText(
                                                                        context,
                                                                        context.getString(
                                                                            R.string
                                                                                .widget_save_error
                                                                        ),
                                                                        Toast.LENGTH_LONG,
                                                                    )
                                                                    .show()
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                                context,
                                                                context.getString(
                                                                    R.string.widget_save_error
                                                                ),
                                                                Toast.LENGTH_LONG,
                                                            )
                                                            .show()
                                                    }
                                                }
                                            }
                                        }
                                        .background(
                                            colorResource(R.color.fill3),
                                            RoundedCornerShape(8.dp),
                                        )
                                        .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = resolved.name, style = Typography.bodySemibold)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun WidgetStopChip(label: String, stopName: String, onClear: () -> Unit) {
    Row(
        modifier =
            Modifier.padding(vertical = 4.dp)
                .background(colorResource(R.color.fill3), RoundedCornerShape(8.dp))
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$label: $stopName", style = Typography.bodySemibold)
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onClear) { Text(stringResource(R.string.widget_clear_stop)) }
    }
}
