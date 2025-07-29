package com.mbta.tid.mbta_app.android.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
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
import kotlin.math.abs

fun Float.toDp(context: Context): Dp = (this / context.resources.displayMetrics.density).dp

@Composable
fun SheetHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleColor: Color = colorResource(R.color.text),
    closeText: String? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    rightActionContents: @Composable (() -> Unit)? = null,
    buttonColors: ButtonColors = ButtonDefaults.contrast(),
) {
    val context = LocalContext.current
    val buttonSize = 32.dp
    val touchTarget = 48.dp
    var hasButtons = onBack != null || onClose != null || rightActionContents != null
    var buttonPadding by remember { mutableStateOf(0.dp) }
    var textPadding by remember { mutableStateOf(0.dp) }

    /**
     * This sets padding for either the buttons or the text, so that the back and close buttons are
     * always aligned with the first line of text, even if the text breaks to multiple lines.
     */
    fun alignButtons(layout: TextLayoutResult) {
        if (hasButtons) {
            val lineHeight = (layout.getLineBottom(0) - layout.getLineTop(0)).toDp(context)
            val padding = (lineHeight / 2) - (touchTarget / 2)
            if (padding < 0.dp) {
                buttonPadding = 0.dp
                textPadding = abs(padding.value).dp
            } else {
                buttonPadding = padding + layout.getLineTop(0).toDp(context)
                textPadding = 0.dp
            }
        }
    }

    Row(
        modifier.padding(horizontal = 16.dp).heightIn(min = touchTarget),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = if (hasButtons) Alignment.Top else Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            ActionButton(
                ActionButtonKind.Back,
                modifier = Modifier.padding(top = buttonPadding),
                size = buttonSize,
                colors = buttonColors,
                action = onBack,
            )
        }
        if (title != null) {
            Text(
                title,
                color = titleColor,
                modifier =
                    Modifier.semantics { heading() }
                        .padding(top = textPadding)
                        .padding(start = if (onBack == null) 8.dp else 0.dp)
                        .weight(1f)
                        .placeholderIfLoading(),
                style = Typography.title2Bold,
                onTextLayout = ::alignButtons,
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (rightActionContents != null) {
            Row(modifier = Modifier.padding(top = buttonPadding)) { rightActionContents() }
        }

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
}

@Preview
@Composable
private fun SheetHeaderPreview() {
    MyApplicationTheme {
        Column(Modifier.background(colorResource(R.color.fill2))) {
            SheetHeader(title = "This is a very long sheet title it should wrap", onClose = {})
            HorizontalDivider()
            SheetHeader(title = "Short", onClose = {})
            HorizontalDivider()
            SheetHeader(title = "No back button")
            HorizontalDivider()
            SheetHeader(title = "Back and close", onBack = {}, onClose = {})
            SheetHeader(
                title = "Back and text close",
                closeText = "Done",
                onBack = {},
                onClose = {},
            )
        }
    }
}
