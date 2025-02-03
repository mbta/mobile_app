package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading

@Composable
fun TransitHeader(
    name: String,
    backgroundColor: Color,
    textColor: Color,
    modeIcon: Painter,
    modeDescription: String?,
    rightContent: (@Composable (textColor: Color) -> Unit)? = null
) {
    Row(
        modifier = Modifier.background(backgroundColor).fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modeIcon,
            contentDescription = modeDescription,
            tint = textColor,
            modifier = Modifier.placeholderIfLoading()
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            maxLines = 1,
            modifier = Modifier.semantics { heading() }.weight(1.0f).placeholderIfLoading(),
            style =
                LocalTextStyle.current.copy(
                    color = textColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
        )
        if (rightContent != null) {
            rightContent(textColor)
        }
    }
}
