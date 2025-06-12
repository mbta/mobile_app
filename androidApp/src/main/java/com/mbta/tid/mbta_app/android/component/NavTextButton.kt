package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography

@Composable
fun NavTextButton(string: String, onTap: () -> Unit) {
    val backgroundColor = colorResource(R.color.text).copy(alpha = 0.6f)
    val textColor = colorResource(R.color.fill2)
    Button(
        onTap,
        colors =
            ButtonColors(
                backgroundColor,
                textColor,
                backgroundColor.copy(alpha = backgroundColor.alpha * 0.2f),
                textColor.copy(alpha = 0.2f),
            ),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.heightIn(min = 32.dp),
    ) {
        Text(string, style = Typography.callout)
    }
}
