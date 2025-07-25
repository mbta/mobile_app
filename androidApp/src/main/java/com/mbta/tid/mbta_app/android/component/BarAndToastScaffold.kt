package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import org.koin.compose.koinInject

@Composable
fun BarAndToastScaffold(
    bottomBar: @Composable () -> Unit = {},
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    toastViewModel: IToastViewModel = koinInject(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val toastState by toastViewModel.models.collectAsState()

    LaunchedEffect(toastState) {
        when (val state = toastState) {
            is ToastViewModel.State.Hidden -> snackbarHostState.currentSnackbarData?.dismiss()
            is ToastViewModel.State.Visible ->
                snackbarHostState.showSnackbar(
                    message = state.toast.message,
                    actionLabel =
                        if (state.toast.actionLabel != null && state.toast.onAction != null)
                            state.toast.actionLabel
                        else null,
                    withDismissAction = state.toast.onClose != null,
                    duration =
                        when (state.toast.duration) {
                            ToastViewModel.Duration.Short -> SnackbarDuration.Short
                            ToastViewModel.Duration.Long -> SnackbarDuration.Long
                            ToastViewModel.Duration.Indefinite -> SnackbarDuration.Indefinite
                        },
                )
        }
    }

    Scaffold(
        bottomBar = bottomBar,
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                Snackbar(
                    modifier = Modifier.padding(start = 8.dp, bottom = 16.dp, end = 8.dp),
                    action = { ToastActionButton(it, toastState) },
                    dismissAction = { ToastCloseButton(it, toastState) },
                    shape = RoundedCornerShape(8.dp),
                    containerColor = colorResource(R.color.contrast),
                    contentColor = Color.Transparent,
                    actionContentColor = Color.Transparent,
                    dismissActionContentColor = Color.Transparent,
                ) {
                    Text(
                        AnnotatedString.fromHtml(it.visuals.message),
                        Modifier.padding(vertical = 8.dp),
                        colorResource(R.color.fill3),
                    )
                }
            }
        },
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

@Composable
private fun ToastActionButton(snackbarData: SnackbarData, toastState: ToastViewModel.State) {
    snackbarData.visuals.actionLabel?.let { label ->
        NavTextButton(
            label,
            modifier =
                Modifier.padding(
                    start = 8.dp,
                    end = if (snackbarData.visuals.withDismissAction) 0.dp else 8.dp,
                ),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.halo_inverse),
                    contentColor = colorResource(R.color.fill3),
                ),
        ) {
            when (toastState) {
                is ToastViewModel.State.Hidden -> {}
                is ToastViewModel.State.Visible ->
                    toastState.toast.onAction?.let { action -> action() }
            }
        }
    }
}

@Composable
private fun ToastCloseButton(snackbarData: SnackbarData, toastState: ToastViewModel.State) {
    if (snackbarData.visuals.withDismissAction)
        ActionButton(
            ActionButtonKind.Close,
            modifier = Modifier.padding(horizontal = 8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.halo_inverse),
                    contentColor = colorResource(R.color.fill3),
                ),
        ) {
            when (toastState) {
                is ToastViewModel.State.Hidden -> {}
                is ToastViewModel.State.Visible ->
                    toastState.toast.onClose?.let { close -> close() }
            }
        }
}
