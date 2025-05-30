package com.mbta.tid.mbta_app.android.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography

@Composable
fun MoreButton(
    label: String,
    action: () -> Unit,
    note: String? = null,
    icon: (@Composable () -> Unit)? = null,
    isKey: Boolean = false,
) {
    Row(
        modifier =
            Modifier.clickable { action() }
                .semantics(mergeDescendants = true) {}
                .background(
                    color =
                        if (isKey) {
                            colorResource(R.color.key)
                        } else {
                            colorResource(R.color.fill3)
                        }
                )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = Typography.body,
                    color =
                        if (isKey) {
                            colorResource(R.color.fill3)
                        } else colorResource(R.color.text),
                )
                if (note != null) {
                    Text(
                        note,
                        modifier = Modifier.padding(top = 2.dp).alpha(0.6f),
                        color =
                            if (isKey) {
                                colorResource(R.color.fill3)
                            } else colorResource(R.color.text),
                        style = Typography.footnote,
                    )
                }
            }

            if (icon != null) {
                icon()
            }
        }
    }
}
