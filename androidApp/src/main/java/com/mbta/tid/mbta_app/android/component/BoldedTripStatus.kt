package com.mbta.tid.mbta_app.android.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun BoldedTripStatus(text: String, modifier: Modifier = Modifier) {
    val textParts = text.split(" ")

    Text(
        text =
            AnnotatedString(
                text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            start = 0,
                            end = textParts[0].length,
                        ),
                        AnnotatedString.Range(
                            SpanStyle(fontWeight = FontWeight.Normal),
                            start = textParts[0].length,
                            end = text.length,
                        ),
                    )
            ),
        modifier = modifier,
        style = MaterialTheme.typography.headlineMedium,
    )
}
