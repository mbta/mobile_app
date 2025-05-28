package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.fromHex
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
    currentFavorites: Map<Int, Boolean>,
    proposedFavoritesToToggle: Set<Int>,
    toggleFavorite: (RouteStopDirection) -> Unit,
    onClose: () -> Unit,
) {

    var favoritesToSave: Map<Int, Boolean> by remember {
        mutableStateOf(
            currentFavorites.plus(
                proposedFavoritesToToggle.associateWith {
                    !currentFavorites.getOrDefault(it, false)
                }
            )
        )
    }

    Dialog(onDismissRequest = {}) {
        Card(modifier = Modifier.fillMaxWidth()) {
            // TODO: translation
            Text("Add ${lineOrRoute.name} at ${stop.name} to Favorites")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    directions.map { direction ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DirectionLabel(direction)
                            PinButton(
                                favoritesToSave.getOrDefault(direction.id, false),
                                color = Color.fromHex(lineOrRoute.backgroundColor),
                            ) {
                                favoritesToSave =
                                    favoritesToSave.plus(
                                        direction.id to
                                            !favoritesToSave.getOrDefault(direction.id, false)
                                    )
                            }
                        }
                    }
                }
            }
            Row() {
                Button(onClose, modifier = Modifier.weight(1.0F)) { Text("Cancel") }
                Button(
                    {
                        favoritesToSave.forEach { (directionId, isFavorite) ->
                            if (currentFavorites.getOrDefault(directionId, false) != isFavorite) {
                                toggleFavorite(
                                    RouteStopDirection(lineOrRoute.id, stop.id, directionId)
                                )
                            }
                        }
                        onClose()
                    },
                    modifier = Modifier.weight(1.0F),
                ) {
                    Text("Add")
                }
            }
        }
    }
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
                currentFavorites = mapOf(0 to false, 1 to false),
                proposedFavoritesToToggle = setOf(0),
                toggleFavorite = {},
                onClose = {},
            )
        }
    }
}
