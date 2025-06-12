package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import org.koin.compose.KoinContext
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@Composable
fun ErrorBanner(vm: ErrorBannerViewModel, modifier: Modifier = Modifier) {
    val state by vm.errorState.collectAsState()
    when (state) {
        is ErrorBannerState.DataError -> {
            ErrorCard(
                modifier,
                details = {
                    Text(
                        stringResource(R.string.error_loading_data),
                        style = Typography.subheadline,
                    )
                    DebugView {
                        Text((state as? ErrorBannerState.DataError)?.messages?.joinToString() ?: "")
                    }
                },
                button = {
                    RefreshButton(label = stringResource(R.string.reload_data)) {
                        (state as ErrorBannerState.DataError).action()
                        vm.clearState()
                    }
                },
            )
        }
        is ErrorBannerState.NetworkError -> {
            ErrorCard(
                modifier,
                details = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.wifi_slash), contentDescription = "")
                        Text(
                            stringResource(R.string.unable_to_connect),
                            modifier = Modifier.padding(start = 12.dp),
                            style = Typography.subheadline,
                        )
                        Spacer(Modifier.weight(1f))
                    }
                },
            )
        }
        is ErrorBannerState.StalePredictions -> {
            if (vm.loadingWhenPredictionsStale) {
                Row(
                    modifier = modifier.heightIn(60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IndeterminateLoadingIndicator(Modifier.width(48.dp))
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                ErrorCard(
                    modifier,
                    details = {
                        val minutes = (state as ErrorBannerState.StalePredictions).minutesAgo()
                        Text(
                            pluralStringResource(
                                R.plurals.updated_mins_ago,
                                minutes.toInt(),
                                minutes,
                            ),
                            style = Typography.subheadline,
                        )
                    },
                    button = {
                        RefreshButton(label = stringResource(R.string.refresh_predictions)) {
                            (state as ErrorBannerState.StalePredictions).action()
                            vm.clearState()
                        }
                    },
                )
            }
        }
        null -> return
    }
}

@Composable
private fun ErrorCard(
    modifier: Modifier = Modifier,
    details: @Composable () -> Unit,
    button: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .padding(horizontal = 16.dp)
                .heightIn(60.dp)
                .background(Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(15.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) { details() }
        if (button != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) { button() }
        }
    }
}

@Composable
private fun RefreshButton(
    loading: Boolean = false,
    label: String = stringResource(R.string.refresh),
    action: () -> Unit,
) {
    TextButton(
        onClick = action,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(20.dp),
    ) {
        Box {
            if (loading) {
                IndeterminateLoadingIndicator()
            } else {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = label,
                    modifier = Modifier.width(20.dp),
                    tint = Color.Unspecified,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ErrorBannerPreviews() {
    @Composable
    fun PreviewBanner(vm: ErrorBannerViewModel) {
        LaunchedEffect(null) { vm.activate() }
        ErrorBanner(vm)
    }

    val dataErrorRepo =
        MockErrorBannerStateRepository(
            state = ErrorBannerState.DataError(messages = setOf("foo"), action = {})
        )
    val dataErrorVM = viewModel(key = "data") { ErrorBannerViewModel(false, dataErrorRepo) }

    val networkErrorRepo = MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
    val networkErrorVM =
        viewModel(key = "network") { ErrorBannerViewModel(false, networkErrorRepo) }

    val staleRepo =
        MockErrorBannerStateRepository(
            state =
                ErrorBannerState.StalePredictions(
                    lastUpdated = Clock.System.now().minus(2.minutes),
                    action = {},
                )
        )
    val staleVM = viewModel(key = "stale") { ErrorBannerViewModel(false, staleRepo) }
    val staleLoadingVM = viewModel(key = "loading") { ErrorBannerViewModel(true, staleRepo) }

    // The preview requires Koin to contain the cache in order to render,
    // but it won't actually use the debug value set here when displayed
    val settingsRepo = MockSettingsRepository(mapOf(Settings.DevDebugMode to false))
    val koinApplication = koinApplication {
        modules(module { single<SettingsCache> { SettingsCache(settingsRepo) } })
    }
    KoinContext(koinApplication.koin) {
        MyApplicationTheme {
            Column(
                modifier =
                    Modifier.background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PreviewBanner(networkErrorVM)
                PreviewBanner(dataErrorVM)
                PreviewBanner(staleVM)
                PreviewBanner(staleLoadingVM)
            }
        }
    }
}
