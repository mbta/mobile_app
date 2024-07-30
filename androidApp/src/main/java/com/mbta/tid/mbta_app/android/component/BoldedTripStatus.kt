package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BoldedTripStatus(text: String, modifier: Modifier) {
    Row(modifier) {
        val textParts = text.split(" ")
        Text(textParts.first(), fontWeight = FontWeight.Bold)
        if (textParts.size > 1) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(textParts.slice(IntRange(start = 1, endInclusive = 1)).joinToString(" "))
        }
    }
}
