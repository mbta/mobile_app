package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.utils.TestData

@Composable
fun FavoriteConfirmationDialog(
    lineOrRoute: RouteCardData.LineOrRoute,
    stop: Stop,
    directions: List<Direction>,
    proposedFavorites: Map<Int, Boolean>,
    updateFavorites: (Map<RouteStopDirection, Boolean>) -> Unit,
    onClose: () -> Unit,
) {

    var favoritesToSave: Map<Int, Boolean> by remember { mutableStateOf(proposedFavorites) }

    AlertDialog(
        containerColor = colorResource(R.color.fill1),
        onDismissRequest = onClose,
        confirmButton = {
            TextButton({
                val newFavorites =
                    favoritesToSave.mapKeys { (directionId, _isFavorite) ->
                        RouteStopDirection(lineOrRoute.id, stop.id, directionId)
                    }
                updateFavorites(newFavorites)
                onClose()
            }) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClose) { Text("Cancel") } },
        title = {
            // TODO: translation
            Text(
                "Add ${lineOrRoute.name} at ${stop.name} to Favorites",
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(modifier = Modifier.haloContainer(1.dp)) {
                directions.mapIndexed { idx, direction ->
                    Button(
                        onClick = {
                            favoritesToSave =
                                favoritesToSave.plus(
                                    direction.id to
                                        !favoritesToSave.getOrDefault(direction.id, false)
                                )
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.fill3),
                                contentColor = colorResource(R.color.text),
                            ),
                        shape = RectangleShape,
                        modifier = Modifier.background(color = Color.White),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DirectionLabel(direction)
                            StarIcon(
                                favoritesToSave.getOrDefault(direction.id, false),
                                color = Color.fromHex(lineOrRoute.backgroundColor),
                            )
                        }
                    }
                    if (idx + 1 < directions.size) {
                        HaloSeparator()
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun FavoriteConfirmationDialogPreview() {

    val objects = TestData

    MyApplicationTheme {
        Column(Modifier.background(colorResource(R.color.fill3))) {
            FavoriteConfirmationDialog(
                RouteCardData.LineOrRoute.Line(objects.getLine("line-Green"), emptySet()),
                stop = objects.getStop("place-boyls"),
                directions =
                    listOf(
                        Direction(id = 0, name = "West", destination = "Copley & West"),
                        Direction(id = 1, name = "East", destination = "Park St & North"),
                    ),
                proposedFavorites = mapOf(0 to true, 1 to false),
                updateFavorites = {},
                onClose = {},
            )
        }
    }
}
