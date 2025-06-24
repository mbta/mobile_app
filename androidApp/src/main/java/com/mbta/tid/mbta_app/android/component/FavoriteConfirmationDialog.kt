package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    proposedFavorites: Map<Int, Boolean>,
    updateFavorites: (Map<RouteStopDirection, Boolean>) -> Unit,
    onClose: () -> Unit,
) {

    var favoritesToSave: Map<Int, Boolean> by remember { mutableStateOf(proposedFavorites) }

    Dialog(onDismissRequest = onClose) {
        Column(
            modifier =
                Modifier.background(colorResource(R.color.fill1), shape = RoundedCornerShape(28.dp))
        ) {
            Text(
                AnnotatedString.fromHtml(
                    stringResource(R.string.add_to_favorites_title, lineOrRoute.name, stop.name)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp).semantics { heading() },
            )
            Column() {
                HaloSeparator()
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
                        modifier =
                            Modifier.background(color = colorResource(R.color.fill3)).semantics {
                                selected = favoritesToSave.getOrDefault(direction.id, false)
                            },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DirectionLabel(direction, Modifier.weight(1f))
                            StarIcon(
                                favoritesToSave.getOrDefault(direction.id, false),
                                color = Color.fromHex(lineOrRoute.backgroundColor),
                                Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                    if (idx + 1 < directions.size) {
                        HaloSeparator()
                    }
                }
                HaloSeparator()
            }

            Row(modifier = Modifier.padding(16.dp)) {
                Spacer(Modifier.weight(1F))
                TextButton(onClose) { Text("Cancel") }
                TextButton({
                    val newFavorites =
                        favoritesToSave.mapKeys { (directionId, _isFavorite) ->
                            RouteStopDirection(lineOrRoute.id, stop.id, directionId)
                        }
                    updateFavorites(newFavorites)
                    onClose()
                }) {
                    Text(stringResource(R.string.add_confirmation_button))
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
                proposedFavorites = mapOf(0 to true, 1 to false),
                updateFavorites = {},
                onClose = {},
            )
        }
    }
}
