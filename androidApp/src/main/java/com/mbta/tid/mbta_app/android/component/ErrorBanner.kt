package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.model.ErrorBannerState
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock

@Composable
fun ErrorBanner(vm: ErrorBannerViewModel) {
    val state by vm.errorState.collectAsState()
    when (state) {
        is ErrorBannerState.DataError -> {
            ErrorCard(
                details = { Text(stringResource(R.string.error_loading_data)) },
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
                details = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription =
                                "Displayed when the phone is not connected to the network"
                        )
                        Text(
                            stringResource(R.string.unable_to_connect),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
            )
        }
        is ErrorBannerState.StalePredictions -> {
            if (vm.loadingWhenPredictionsStale) {
                Row(
                    modifier = Modifier.heightIn(60.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IndeterminateLoadingIndicator()
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else {
                ErrorCard(
                    details = {
                        val minutes = (state as ErrorBannerState.StalePredictions).minutesAgo()
                        Text(stringResource(R.string.updated_mins_ago, minutes))
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
private fun ErrorCard(details: @Composable () -> Unit, button: (@Composable () -> Unit)? = null) {
    Row(
        modifier =
            Modifier.padding(16.dp)
                .heightIn(60.dp)
                .background(Color.Gray.copy(alpha = 0.1f))
                .clip(RoundedCornerShape(15.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp)) { details() }
        Spacer(Modifier.weight(1f))
        if (button != null) {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) { button() }
        }
    }
}

@Composable
private fun RefreshButton(
    loading: Boolean = false,
    label: String = stringResource(R.string.refresh),
    action: () -> Unit
) {
    Button(onClick = action) {
        Box {
            if (loading) {
                IndeterminateLoadingIndicator()
            } else {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = label,
                    modifier = Modifier.width(20.dp)
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
            state = ErrorBannerState.DataError(messages = emptySet(), action = {})
        )
    val dataErrorVM = ErrorBannerViewModel(false, dataErrorRepo, MockSettingsRepository())
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
    LaunchedEffect(null) { staleVM.activate() }
    LaunchedEffect(null) { staleLoadingVM.activate() }
    Column(verticalArrangement = Arrangement.SpaceEvenly) {
        ErrorBanner(networkErrorVM)
        ErrorBanner(dataErrorVM)
        ErrorBanner(staleVM)
        ErrorBanner(staleLoadingVM)
    }
}
