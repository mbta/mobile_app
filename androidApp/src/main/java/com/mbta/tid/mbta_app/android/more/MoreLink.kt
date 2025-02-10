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
fun MoreLink(label: String, url: String, note: String? = null, isKey: Boolean = false) {
    val context = LocalContext.current

    @Composable
    fun LinkIcon() {
        Icon(
            painterResource(R.drawable.arrow_up_right),
            contentDescription = stringResource(id = R.string.more_link_external),
            modifier = Modifier.size(12.dp),
            tint =
                if (isKey) {
                    colorResource(R.color.fill3)
                } else colorResource(R.color.deemphasized),
        )
    }

    MoreButton(
        label,
        {
            val webpage: Uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, webpage)
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Log.i("More", "Failed to navigate to link on MoreLink click")
            }
        },
        note,
        { LinkIcon() },
        isKey,
    )
}
