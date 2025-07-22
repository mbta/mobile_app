package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.contrastTranslucent

@Composable
fun NavTextButton(
    string: String,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
    colors: ButtonColors = ButtonDefaults.contrastTranslucent(),
    action: () -> Unit,
) {
    Button(
        action,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = modifier.heightIn(min = height).widthIn(min = 64.dp),
    ) {
        Text(string, style = Typography.callout, maxLines = 1)
    }
}
