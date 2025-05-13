package com.mbta.tid.mbta_app.android.pages

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.SheetRoutes

@Composable
fun FavoritesPage(modifier: Modifier = Modifier, openSheetRoute: (SheetRoutes) -> Unit) {
    Text(stringResource(R.string.favorites_link))
}
