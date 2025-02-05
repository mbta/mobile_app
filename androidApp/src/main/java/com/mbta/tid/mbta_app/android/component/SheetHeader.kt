package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.util.modifiers.placeholderIfLoading

@Composable
fun SheetHeader(title: String? = null, onClose: (() -> Unit)? = null) {
    Row(
        Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (title != null) {
            Text(
                title,
                modifier = Modifier.semantics { heading() }.weight(1f).placeholderIfLoading(),
                style = MaterialTheme.typography.titleSmall
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (onClose != null) {
            ActionButton(ActionButtonKind.Close, action = onClose)
        }
    }
}

@Preview
@Composable
private fun SheetHeaderPreview() {
    MyApplicationTheme {
        Column {
            SheetHeader(onClose = {}, title = "This is a very long sheet title it should wrap")
            HorizontalDivider()
            SheetHeader(onClose = {}, title = "short")
            HorizontalDivider()
            SheetHeader(title = "no back button")
        }
    }
}
