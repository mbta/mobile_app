package com.mbta.tid.mbta_app.android.map

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.routeIcon
import com.mbta.tid.mbta_app.model.RouteType

@Composable
fun TripCenterButton(modifier: Modifier = Modifier, routeType: RouteType, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier.semantics(mergeDescendants = true) {},
        colors =
            IconButtonDefaults.iconButtonColors(
                containerColor = colorResource(R.color.fill3),
                contentColor = colorResource(R.color.key)
            )
    ) {
        val painter = routeIcon(routeType).first
        Icon(painter, stringResource(R.string.recenter))
    }
}
