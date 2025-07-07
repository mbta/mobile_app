package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R

@Composable
fun <T> RoundedCornerColumn(
    data: List<T>,
    modifier: Modifier = Modifier,
    element: @Composable (shape: RoundedCornerShape, item: T) -> Unit,
) {
    Column(modifier) {
        data.forEachIndexed { index, item ->
            val shape =
                if (data.size == 1) {
                    RoundedCornerShape(10.dp)
                } else if (index == 0) {
                    RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                } else if (index == data.lastIndex) {
                    RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                } else {
                    RoundedCornerShape(0.dp)
                }
            if (index != 0) {
                HorizontalDivider(color = colorResource(R.color.fill1))
            }
            element(shape, item)
        }
    }
}
