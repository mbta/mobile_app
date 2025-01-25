package com.mbta.tid.mbta_app.android.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading

enum class ActionButtonKind(
    val iconSize: Dp,
    @StringRes val accessibilityLabel: Int,
    @DrawableRes val image: Int
) {
    Back(14.dp, R.string.back_button_label, R.drawable.fa_chevron_left),
    Close(10.dp, R.string.close_button_label, R.drawable.fa_xmark)
}

@Composable
fun ActionButton(kind: ActionButtonKind, size: Dp = 32.dp, action: () -> Unit) {
    Button(
        onClick = action,
        modifier = Modifier.size(size).width(size).placeholderIfLoading(),
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.contrast),
                contentColor = colorResource(R.color.fill2)
            ),
        contentPadding = PaddingValues(5.dp)
    ) {
        Icon(
            painterResource(kind.image),
            stringResource(kind.accessibilityLabel),
            Modifier.size(kind.iconSize)
        )
    }
}

@Preview(name = "Back Button")
@Composable
private fun BackButtonPreview() {
    ActionButton(kind = ActionButtonKind.Back) { println("Pressed") }
}

@Preview(name = "Close Button")
@Composable
private fun CloseButtonPreview() {
    ActionButton(kind = ActionButtonKind.Close) { println("Pressed") }
}
