package com.mbta.tid.mbta_app.android.stopDetails

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography

@Composable
fun StopDetailsIconCard(
    accentColor: Color,
    details: (@Composable() () -> Unit)?,
    header: @Composable() (modifier: Modifier) -> Unit,
    icon: @Composable() (modifier: Modifier) -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .border(1.dp, colorResource(R.color.halo), shape = RoundedCornerShape(8.dp))
                .background(colorResource(R.color.fill3))
                .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides accentColor) {
                icon(Modifier.requiredSize(35.dp))
            }
            ProvideTextStyle(Typography.title2Bold) {
                header(Modifier.semantics { heading() }.weight(1f))
            }
        }

        if (details != null) {
            HorizontalDivider(color = accentColor.copy(alpha = 0.25f))
            CompositionLocalProvider(LocalTextStyle provides Typography.callout) { details() }
        }
    }
}
