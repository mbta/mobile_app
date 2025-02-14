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
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

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
                        style = MaterialTheme.typography.headlineSmall
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
                }
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
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
            )
        }
        is ErrorBannerState.StalePredictions -> {
            if (vm.loadingWhenPredictionsStale) {
                Row(
                    modifier = modifier.heightIn(60.dp),
                    verticalAlignment = Alignment.CenterVertically
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
                                minutes
                            ),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    button = {
                        RefreshButton(label = stringResource(R.string.refresh_predictions)) {
                            (state as ErrorBannerState.StalePredictions).action()
                            vm.clearState()
                        }
                    }
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
    button: (@Composable () -> Unit)? = null
) {
    Row(
        modifier =
            modifier
                .padding(horizontal = 16.dp)
                .heightIn(60.dp)
                .background(Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(15.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) { details() }
        Spacer(Modifier.weight(1f))
        if (button != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) { button() }
        }
    }
}

@Composable
private fun RefreshButton(
    loading: Boolean = false,
    label: String = stringResource(R.string.refresh),
    action: () -> Unit
) {
    TextButton(
        onClick = action,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(20.dp)
    ) {
        Box {
            if (loading) {
                IndeterminateLoadingIndicator()
            } else {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = label,
                    modifier = Modifier.width(20.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Preview
@Composable
private fun ErrorBannerPreviews() {
    val networkErrorRepo = MockErrorBannerStateRepository(state = ErrorBannerState.NetworkError {})
    val networkErrorVM = ErrorBannerViewModel(false, networkErrorRepo, MockSettingsRepository())
    val dataErrorRepo =
        MockErrorBannerStateRepository(
            state = ErrorBannerState.DataError(messages = setOf("foo"), action = {})
        )
    val dataErrorVM = ErrorBannerViewModel(false, dataErrorRepo, MockSettingsRepository())
    val dataErrorDebugVM =
        ErrorBannerViewModel(
            false,
            dataErrorRepo,
            MockSettingsRepository(mapOf(Settings.DevDebugMode to true))
        )
    val staleRepo =
        MockErrorBannerStateRepository(
            state =
                ErrorBannerState.StalePredictions(
                    lastUpdated = Clock.System.now().minus(2.minutes),
                    action = {}
                )
        )
    val staleVM = ErrorBannerViewModel(false, staleRepo, MockSettingsRepository())
    val staleLoadingVM = ErrorBannerViewModel(true, staleRepo, MockSettingsRepository())
    LaunchedEffect(null) { networkErrorVM.activate() }
    LaunchedEffect(null) { dataErrorVM.activate() }
    LaunchedEffect(null) { dataErrorDebugVM.activate() }
    LaunchedEffect(null) { staleVM.activate() }
    LaunchedEffect(null) { staleLoadingVM.activate() }
    Column(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ErrorBanner(networkErrorVM)
        ErrorBanner(dataErrorVM)
        ErrorBanner(dataErrorDebugVM)
        ErrorBanner(staleVM)
        ErrorBanner(staleLoadingVM)
    }
}
