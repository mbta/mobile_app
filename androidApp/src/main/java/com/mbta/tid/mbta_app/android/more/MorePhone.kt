package com.mbta.tid.mbta_app.android.more

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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

    @Composable
    fun PhoneIcon() {
        Icon(
            painterResource(R.drawable.fa_phone),
            contentDescription = stringResource(id = R.string.more_link_call),
            modifier = Modifier.size(12.dp),
            tint = colorResource(R.color.deemphasized),
        )
    }

    MoreButton(
        label,
        {
            val numberUri = Uri.parse("tel:$phoneNumber")
            val intent = Intent(Intent.ACTION_DIAL).apply { data = numberUri }
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Log.i("More", "Failed to dial number on MorePhone click")
            }
        },
        icon = { PhoneIcon() }
    )
}
