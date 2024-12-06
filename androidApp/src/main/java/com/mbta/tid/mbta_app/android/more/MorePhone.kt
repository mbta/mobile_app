package com.mbta.tid.mbta_app.android.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun MorePhone(label: String, phoneNumber: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column() {
            Text(
                buildAnnotatedString {
                    withLink(LinkAnnotation.Url("tel:${phoneNumber}")) { append(label) }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Icon(
            painterResource(R.drawable.fa_phone),
            contentDescription = stringResource(id = R.string.icon_description_external_link),
            modifier = Modifier.size(12.dp),
            tint = colorResource(R.color.deemphasized),
        )
    }
}
