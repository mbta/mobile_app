package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.contrast
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading
import com.mbta.tid.mbta_app.utils.NavigationCallbacks
import kotlin.math.abs

@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: @Composable RowScope.() -> Unit = { Spacer(Modifier.weight(1f)) },
    closeText: String? = null,
    navCallbacks: NavigationCallbacks,
    rightActionContents: @Composable (RowScope.() -> Unit)? = null,
    buttonColors: ButtonColors = ButtonDefaults.contrast(),
    buttonPadding: Dp = 0.dp,
) {
    val buttonSize = 32.dp
    val touchTarget = 48.dp
    val hasButtons =
        (navCallbacks.onBack != null &&
            navCallbacks.backButtonPresentation !=
                NavigationCallbacks.BackButtonPresentation.Floating) ||
            navCallbacks.onClose != null ||
            rightActionContents != null

    @Composable
    fun BackButton() {
        val onBack = navCallbacks.onBack
        if (
            onBack != null &&
                navCallbacks.backButtonPresentation !=
                    NavigationCallbacks.BackButtonPresentation.Floating
        ) {
            ActionButton(
                ActionButtonKind.Back,
                modifier = Modifier.padding(top = buttonPadding),
                size = buttonSize,
                colors = buttonColors,
                action = onBack,
            )
        }
    }

    @Composable
    fun RowScope.RightActionContents() {
        if (rightActionContents != null) {
            Row(
                modifier = Modifier.padding(top = buttonPadding).align(Alignment.CenterVertically)
            ) {
                rightActionContents()
            }
        }
    }

    @Composable
    fun CloseButton() {
        val onClose = navCallbacks.onClose
        if (onClose != null) {
            if (closeText != null)
                NavTextButton(
                    closeText,
                    modifier = Modifier.padding(top = buttonPadding),
                    colors = buttonColors,
                    height = buttonSize,
                    action = onClose,
                )
            else
                ActionButton(
                    ActionButtonKind.Close,
                    modifier = Modifier.padding(top = buttonPadding),
                    size = buttonSize,
                    colors = buttonColors,
                    action = onClose,
                )
        }
    }

    // the back button is always floating, so if it’s in the sheet header, we have
    if (
        navCallbacks.onBack != null &&
            navCallbacks.backButtonPresentation !=
                NavigationCallbacks.BackButtonPresentation.Floating &&
            navCallbacks.onClose != null
    ) {
        Column(modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.heightIn(min = touchTarget).fillMaxWidth()) {
                BackButton()
                Spacer(Modifier.weight(1f))
                RightActionContents()
                CloseButton()
            }
            Row { title() }
        }
    } else {
        Row(
            modifier.padding(horizontal = 16.dp).heightIn(min = touchTarget),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = if (hasButtons) Alignment.Top else Alignment.CenterVertically,
        ) {
            BackButton()
            title()
            RightActionContents()
            CloseButton()
        }
    }
}

@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleContentDescription: String? = null,
    titleColor: Color = colorResource(R.color.text),
    closeText: String? = null,
    navCallbacks: NavigationCallbacks,
    rightActionContents: @Composable (RowScope.() -> Unit)? = null,
    buttonColors: ButtonColors = ButtonDefaults.contrast(),
) {
    val density = LocalDensity.current
    val touchTarget = 48.dp
    val hasButtons =
        (navCallbacks.onBack != null &&
            navCallbacks.backButtonPresentation !=
                NavigationCallbacks.BackButtonPresentation.Floating) ||
            navCallbacks.onClose != null ||
            rightActionContents != null
    var buttonPadding by remember { mutableStateOf(0.dp) }
    var textPadding by remember { mutableStateOf(0.dp) }

    /**
     * This sets padding for either the buttons or the text, so that the back and close buttons are
     * always aligned with the first line of text, even if the text breaks to multiple lines.
     */
    fun alignButtons(layout: TextLayoutResult) =
        with(density) {
            if (hasButtons) {
                val lineHeight = (layout.getLineBottom(0) - layout.getLineTop(0)).toDp()
                val padding = (lineHeight / 2) - (touchTarget / 2)
                if (padding < 0.dp) {
                    buttonPadding = 0.dp
                    textPadding = abs(padding.value).dp
                } else {
                    buttonPadding = padding + layout.getLineTop(0).toDp()
                    textPadding = 0.dp
                }
            }
        }

    SheetHeader(
        modifier,
        title = {
            if (title != null) {
                Text(
                    title,
                    color = titleColor,
                    modifier =
                        Modifier.semantics {
                                heading()
                                contentDescription = titleContentDescription ?: title
                            }
                            .padding(top = textPadding)
                            .padding(
                                start =
                                    if (
                                        navCallbacks.onBack == null ||
                                            navCallbacks.backButtonPresentation ==
                                                NavigationCallbacks.BackButtonPresentation
                                                    .Floating ||
                                            navCallbacks.onClose != null
                                    )
                                        8.dp
                                    else 0.dp
                            )
                            .weight(1f)
                            .placeholderIfLoading(),
                    style = Typography.title2Bold,
                    onTextLayout = ::alignButtons,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        },
        closeText,
        navCallbacks,
        rightActionContents,
        buttonColors,
        buttonPadding,
    )
}

@Preview
@Composable
private fun SheetHeaderPreview() {
    MyApplicationTheme {
        Column(Modifier.background(colorResource(R.color.fill2))) {
            SheetHeader(
                title = "This is a very long sheet title it should wrap",
                navCallbacks =
                    NavigationCallbacks(
                        onBack = {},
                        onClose = {},
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                    ),
            )
            HorizontalDivider()
            SheetHeader(
                title = "Short",
                navCallbacks =
                    NavigationCallbacks(
                        onBack = {},
                        onClose = {},
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                    ),
            )
            HorizontalDivider()
            SheetHeader(
                title = "No back button",
                navCallbacks =
                    NavigationCallbacks(
                        onBack = null,
                        onClose = null,
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Floating,
                    ),
            )
            HorizontalDivider()
            SheetHeader(
                title = "Back and close",
                navCallbacks =
                    NavigationCallbacks(
                        onBack = {},
                        onClose = {},
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Header,
                    ),
            )
            HorizontalDivider()
            SheetHeader(
                title = "Back and text close",
                closeText = "Done",
                navCallbacks =
                    NavigationCallbacks(
                        onBack = {},
                        onClose = {},
                        backButtonPresentation = NavigationCallbacks.BackButtonPresentation.Header,
                    ),
            )
        }
    }
}
