package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.MyApplicationTheme

@Composable
fun SheetHeader(onClose: (() -> Unit)? = null, title: String? = null) {
    Row(
        Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (onClose != null) {
            ActionButton(ActionButtonKind.Back, onClose)
        }
        if (title != null) {
            Text(
                title,
                modifier = Modifier.padding(top = 1.dp).semantics { heading() },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
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
