package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TransitHeader(
    name: String,
    backgroundColor: Color,
    textColor: Color,
    modeIcon: Painter,
    modeDescription: String,
    rightContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier =
            Modifier.heightIn(min = 44.dp).background(color = backgroundColor).fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(modeIcon, contentDescription = modeDescription, tint = textColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                maxLines = 1,
                style =
                    LocalTextStyle.current.copy(
                        color = textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
            )
            if (rightContent != null) {
                Spacer(modifier = Modifier.weight(1.0F))
                rightContent()
            }
        }
    }
}
