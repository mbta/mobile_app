package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
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

    suspend fun showToast(toast: ToastViewModel.Toast) {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            message = toast.message,
            actionLabel =
                when (val buttonSpec = toast.action) {
                    is ToastViewModel.ToastAction.Custom -> buttonSpec.actionLabel
                    else -> null
                },
            withDismissAction =
                when (toast.action) {
                    is ToastViewModel.ToastAction.Close -> true
                    else -> false
                },
            duration =
                when (toast.duration) {
                    ToastViewModel.Duration.Short -> SnackbarDuration.Short
                    ToastViewModel.Duration.Long -> SnackbarDuration.Long
                    ToastViewModel.Duration.Indefinite -> SnackbarDuration.Indefinite
                },
        )
    }

    LaunchedEffect(toastState) {
        when (val state = toastState) {
            is ToastViewModel.State.Hidden -> snackbarHostState.currentSnackbarData?.dismiss()
            is ToastViewModel.State.Visible -> showToast(state.toast)
        }
    }

    val closeHintText = stringResource(R.string.close_button_label)

    // Overriding click semantics so that with voice over turned on,
    // the snackbar is read as a single element with a hint to perform the single associated
    // action (whether close or custom)
    val overriddenContentDescription =
        when (val state = toastState) {
            is ToastViewModel.State.Hidden -> ""
            is ToastViewModel.State.Visible ->
                AnnotatedString.fromHtml(
                        if (state.toast.isTip)
                            stringResource(R.string.toast_tip_prefix, state.toast.message)
                        else state.toast.message
                    )
                    .text
        }

    val clickLabel =
        when (val state = toastState) {
            is ToastViewModel.State.Hidden -> ""
            is ToastViewModel.State.Visible ->
                when (val buttonSpec = state.toast.action) {
                    is ToastViewModel.ToastAction.Close -> closeHintText
                    is ToastViewModel.ToastAction.Custom -> buttonSpec.actionLabel
                    null -> ""
                }
        }
    val clickAction: (() -> Unit)? =
        when (val state = toastState) {
            is ToastViewModel.State.Hidden -> null
            is ToastViewModel.State.Visible ->
                when (val buttonSpec = state.toast.action) {
                    is ToastViewModel.ToastAction.Close -> buttonSpec.onClose
                    is ToastViewModel.ToastAction.Custom -> buttonSpec.onAction
                    null -> null
                }
        }

    Scaffold(
        bottomBar = bottomBar,
        snackbarHost = {
            SnackbarHost(snackbarHostState) {
                Snackbar(
                    modifier =
                        Modifier.padding(start = 8.dp, bottom = 16.dp, end = 8.dp)
                            .navigationBarsPadding()
                            .clearAndSetSemantics {
                                contentDescription = overriddenContentDescription
                                onClick(clickLabel) {
                                    clickAction?.invoke()
                                    true
                                }
                            },
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
                    when (val buttonSpec = toastState.toast.action) {
                        is ToastViewModel.ToastAction.Custom -> buttonSpec.onAction()
                        else -> {}
                    }
            }
        }
    }
}

@Composable
private fun ToastCloseButton(snackbarData: SnackbarData, toastState: ToastViewModel.State) {
    if (snackbarData.visuals.withDismissAction)
        ActionButton(
            ActionButtonKind.Dismiss,
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
                    when (val buttonSpec = toastState.toast.action) {
                        is ToastViewModel.ToastAction.Close -> buttonSpec.onClose()
                        else -> {}
                    }
            }
        }
}
