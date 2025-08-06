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
import androidx.compose.ui.platform.LocalContext
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
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fromHex
import com.mbta.tid.mbta_app.android.util.getLabels
import com.mbta.tid.mbta_app.model.Direction
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import org.koin.compose.koinInject

@Composable
fun SaveFavoritesFlow(
    lineOrRoute: RouteCardData.LineOrRoute,
    stop: Stop,
    directions: List<Direction>,
    selectedDirection: Int,
    context: EditFavoritesContext,
    global: GlobalResponse?,
    toastViewModel: IToastViewModel = koinInject(),
    isFavorite: (routeStopDirection: RouteStopDirection) -> Boolean,
    updateFavorites: (Map<RouteStopDirection, Boolean>) -> Unit,
    onClose: () -> Unit,
) {

    val isUnFavoriting =
        directions.any { it.id == selectedDirection } &&
            isFavorite(RouteStopDirection(lineOrRoute.id, stop.id, selectedDirection))

    val isBusOneDirection = directions.size == 1 && lineOrRoute.sortRoute.type == RouteType.BUS
    val localContext = LocalContext.current

    fun updateAndToast(update: Map<RouteStopDirection, Boolean>) {
        updateFavorites(update)
        val favorited = update.filter { it.value }
        val firstFavorite = favorited.entries.firstOrNull() ?: return
        val labels = firstFavorite.key.getLabels(global, localContext)
        var toastText: String? = null

        // If there's only a single favorite, show direction, route, and stop in the toast
        if (favorited.size == 1) {
            toastText =
                labels?.let {
                    localContext.getString(
                        R.string.favorites_toast_add,
                        it.direction,
                        it.route,
                        it.stop,
                    )
                } ?: localContext.getString(R.string.favorites_toast_add_fallback)
        }
        // If there are two favorites and they both have the same route and stop, omit direction
        else if (
            favorited.size == 2 &&
                favorited.keys.all {
                    it.route == firstFavorite.key.route && it.stop == firstFavorite.key.stop
                }
        ) {
            toastText =
                labels?.let {
                    localContext.getString(R.string.favorites_toast_add_multi, it.route, it.stop)
                } ?: localContext.getString(R.string.favorites_toast_add_fallback)
        }

        toastText?.let {
            toastViewModel.showToast(ToastViewModel.Toast(it, ToastViewModel.Duration.Short))
        }
    }

    // Save automatically without confirmation modal
    if (isUnFavoriting || isBusOneDirection && directions.any { it.id == selectedDirection }) {
        val rsd = RouteStopDirection(lineOrRoute.id, stop.id, selectedDirection)
        updateAndToast(mapOf(rsd to !isFavorite(rsd)))

        onClose()
    } else {
        FavoriteConfirmationDialog(
            lineOrRoute,
            stop,
            directions,
            selectedDirection,
            context,
            proposedFavorites =
                directions.associateBy({ it.id }) {
                    // if selectedDirection and already a favorite, then removing favorite.
                    // if not selected direction and already a favorite, then keep it.
                    ((it.id == selectedDirection) xor
                        isFavorite(RouteStopDirection(lineOrRoute.id, stop.id, it.id))) ||
                        // If the only direction is the opposite one, mark it as favorite
                        // whether or not it already is
                        (directions.size == 1 && it.id != selectedDirection)
                },
            updateFavorites = ::updateAndToast,
            onClose = onClose,
        )
    }
}

@Composable
fun FavoriteConfirmationDialog(
    lineOrRoute: RouteCardData.LineOrRoute,
    stop: Stop,
    directions: List<Direction>,
    selectedDirection: Int,
    context: EditFavoritesContext,
    proposedFavorites: Map<Int, Boolean>,
    updateFavorites: (Map<RouteStopDirection, Boolean>) -> Unit,
    onClose: () -> Unit,
) {

    var favoritesToSave: Map<Int, Boolean> by remember { mutableStateOf(proposedFavorites) }

    fun saveAndClose() {
        val newFavorites =
            favoritesToSave.mapKeys { (directionId, _isFavorite) ->
                RouteStopDirection(lineOrRoute.id, stop.id, directionId)
            }
        updateFavorites(newFavorites)
        onClose()
    }

    Dialog(onDismissRequest = onClose) {
        Column(
            modifier =
                Modifier.background(colorResource(R.color.fill1), shape = RoundedCornerShape(28.dp))
        ) {
            val headerText =
                if (context == EditFavoritesContext.Favorites) {
                    stringResource(R.string.add_to_favorites_title, lineOrRoute.name, stop.name)
                } else {
                    stringResource(
                        R.string.add_to_favorites_title_stop_details,
                        lineOrRoute.name,
                        stop.name,
                    )
                }

            Text(
                AnnotatedString.fromHtml(headerText),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp).fillMaxWidth().semantics { heading() },
            )
            if (directions.size == 1 && directions.first().id != selectedDirection) {

                Text(
                    stringResource(
                        R.string.direction_service_only,
                        stringResource(directionNameFormatted(directions.first())),
                    ),
                    textAlign = TextAlign.Center,
                    style = Typography.footnoteSemibold,
                    modifier =
                        Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp).fillMaxWidth(),
                )
            }

            if (directions.isEmpty()) {
                Column {
                    Text(
                        stringResource(R.string.this_stop_is_drop_off_only),
                        textAlign = TextAlign.Center,
                        style = Typography.footnoteSemibold,
                        modifier =
                            Modifier.padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                                .fillMaxWidth(),
                    )
                    HaloSeparator()
                    Row(modifier = Modifier.padding(16.dp)) {
                        Spacer(Modifier.weight(1F))
                        TextButton(onClose) { Text(stringResource(R.string.okay)) }
                    }
                }
            } else {
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
                                Modifier.background(color = colorResource(R.color.fill3))
                                    .semantics {
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
                        HaloSeparator()
                    }
                }

                Row(modifier = Modifier.padding(16.dp)) {
                    Spacer(Modifier.weight(1F))
                    TextButton(onClose) { Text(stringResource(R.string.cancel)) }
                    TextButton({ saveAndClose() }, enabled = favoritesToSave.values.any { it }) {
                        Text(stringResource(R.string.add_confirmation_button))
                    }
                }
            }
        }
    }
}

class Previews() {
    val objects = TestData

    @Preview(name = "Favorites both directions available")
    @Composable
    fun FavoriteConfirmationBoth() {
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
                    selectedDirection = 0,
                    context = EditFavoritesContext.StopDetails,
                    proposedFavorites = mapOf(0 to true, 1 to false),
                    updateFavorites = {},
                    onClose = {},
                )
            }
        }
    }

    @Preview(name = "Favorites selected direction available")
    @Composable
    fun FavoriteConfirmationOnlySelectedDirection() {
        MyApplicationTheme {
            Column(Modifier.background(colorResource(R.color.fill3))) {
                FavoriteConfirmationDialog(
                    RouteCardData.LineOrRoute.Line(objects.getLine("line-Green"), emptySet()),
                    stop = objects.getStop("place-unsqu"),
                    directions =
                        listOf(Direction(id = 0, name = "West", destination = "Copley & West")),
                    selectedDirection = 0,
                    context = EditFavoritesContext.StopDetails,
                    proposedFavorites = mapOf(0 to true),
                    updateFavorites = {},
                    onClose = {},
                )
            }
        }
    }

    @Preview(name = "Favorites opposite direction available")
    @Composable
    fun FavoriteConfirmationOnlyOppositeDirection() {
        MyApplicationTheme {
            Column(Modifier.background(colorResource(R.color.fill3))) {
                FavoriteConfirmationDialog(
                    RouteCardData.LineOrRoute.Line(objects.getLine("line-Green"), emptySet()),
                    stop = objects.getStop("place-unsqu"),
                    directions =
                        listOf(Direction(id = 0, name = "West", destination = "Copley & West")),
                    selectedDirection = 1,
                    context = EditFavoritesContext.Favorites,
                    proposedFavorites = mapOf(0 to true),
                    updateFavorites = {},
                    onClose = {},
                )
            }
        }
    }

    @Preview(name = "Favorites none added button disables")
    @Composable
    fun FavoriteConfirmationDisabledAdd() {
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
                    selectedDirection = 1,
                    context = EditFavoritesContext.Favorites,
                    proposedFavorites = mapOf(0 to false, 1 to false),
                    updateFavorites = {},
                    onClose = {},
                )
            }
        }
    }
}
