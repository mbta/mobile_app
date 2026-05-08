package com.mbta.tid.mbta_app.android.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.ActionButton
import com.mbta.tid.mbta_app.android.component.ActionButtonKind
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.key

@Composable
fun NotificationsHint(onHintTap: () -> Unit, onHintDismiss: () -> Unit) {
    val keyColor = colorResource(R.color.key)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), Arrangement.Top, Alignment.End) {
        Icon(
            painterResource(R.drawable.nub),
            null,
            Modifier.padding(end = 20.dp, bottom = 0.dp).size(width = 24.dp, height = 10.dp),
            tint = keyColor,
        )

        Row(
            Modifier.background(keyColor, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onHintTap)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            Arrangement.spacedBy(8.dp),
            Alignment.CenterVertically,
        ) {
            Text(
                AnnotatedString.fromHtml(stringResource(R.string.notifications_favorites_hint)),
                Modifier.weight(1f),
                color = colorResource(R.color.fill3),
                style = Typography.body,
            )
            ActionButton(
                ActionButtonKind.Dismiss,
                border = BorderStroke(2.dp, colorResource(R.color.halo_inverse)),
                size = 36.dp,
                colors = ButtonDefaults.key(),
                action = onHintDismiss,
            )
        }
    }
}
