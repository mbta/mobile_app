package com.mbta.tid.mbta_app.android.more

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun MorePhone(label: String, phoneNumber: String) {
    val context = LocalContext.current

    Row(
        modifier =
            Modifier.clickable {
                    val numberUri = Uri.parse("tel:$phoneNumber")
                    val intent = Intent(Intent.ACTION_DIAL).apply { data = numberUri }
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Log.i("More", "Failed to dial number on MorePhone click")
                    }
                }
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column { Text(label, style = MaterialTheme.typography.bodyMedium) }
        Icon(
            painterResource(R.drawable.fa_phone),
            contentDescription = stringResource(id = R.string.icon_description_external_link),
            modifier = Modifier.size(12.dp),
            tint = colorResource(R.color.deemphasized),
        )
    }
}
