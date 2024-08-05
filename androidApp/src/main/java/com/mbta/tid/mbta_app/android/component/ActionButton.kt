package com.mbta.tid.mbta_app.android.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

enum class ActionButtonKind(
    val iconSize: Dp,
    val accessibilityLabel: String,
    @DrawableRes val image: Int
) {
    Back(14.dp, "Back", R.drawable.fa_chevron_left),
    Close(10.dp, "Close", R.drawable.fa_xmark)
}

@Composable
fun ActionButton(kind: ActionButtonKind, action: () -> Unit) {
    Button(
        onClick = action,
        modifier = Modifier.size(32.dp),
        shape = CircleShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.contrast),
                contentColor = colorResource(R.color.fill2)
            ),
        contentPadding = PaddingValues(5.dp)
    ) {
        Icon(painterResource(kind.image), kind.accessibilityLabel, Modifier.size(kind.iconSize))
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
