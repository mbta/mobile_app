package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.NavTextButton
import com.mbta.tid.mbta_app.android.component.stopCard.FavoriteStopCard
import com.mbta.tid.mbta_app.android.favorites.NotificationSettingsWidget
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.getLabels
import com.mbta.tid.mbta_app.android.util.key
import com.mbta.tid.mbta_app.android.util.manageFavorites
import com.mbta.tid.mbta_app.android.util.notificationPermissionState
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockSubscriptionsRepository
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.MockToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.koinInject
import org.koin.dsl.koinApplication
import org.koin.dsl.module

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SaveFavoritePage(
    routeId: LineOrRoute.Id,
    stopId: String,
    initialDirection: Int,
    context: EditFavoritesContext,
    goBack: () -> Unit,
    toastViewModel: IToastViewModel = koinInject(),
) {
    val coroutineScope = rememberCoroutineScope()
    val localContext = LocalContext.current

    val global = getGlobalData("SaveFavoritePage")
    val lineOrRoute = global?.getLineOrRoute(routeId)
    val stop = global?.getStop(stopId)

    val (favorites, updateFavorites) = manageFavorites()

    if (global == null || lineOrRoute == null || stop == null) {
        Text("Loading")
        return
    }
    val allPatternsForStop = global.getPatternsFor(stop.id, lineOrRoute)
    val stopDirections =
        lineOrRoute.directions(global, stop, allPatternsForStop.filter { it.isTypical() }).filter {
            it.id in
                RouteDetailsStopList.RouteParameters(lineOrRoute, global).availableDirections &&
                !stop.isLastStopForAllPatterns(it.id, allPatternsForStop, global)
        }

    var selectedDirection by remember {
        mutableIntStateOf(stopDirections.singleOrNull()?.id ?: initialDirection)
    }
    val selectedRouteStopDirection = RouteStopDirection(routeId, stopId, selectedDirection)

    val isFavorite = favorites?.containsKey(selectedRouteStopDirection) ?: false

    val existingSettings = favorites?.get(selectedRouteStopDirection) ?: FavoriteSettings()
    var updatedSettings by
        rememberSaveable(saver = stateJsonSaver()) { mutableStateOf<FavoriteSettings?>(null) }
    val settings = updatedSettings ?: existingSettings

    val notificationPermissionState = notificationPermissionState()

    fun updateCloseAndToast(update: Map<RouteStopDirection, FavoriteSettings?>) {
        notificationPermissionState.launchPermissionRequest()
        coroutineScope.launch {
            updateFavorites(update, context, selectedDirection)
            val favorited = update.filter { it.value != null }
            val firstFavorite = favorited.entries.firstOrNull()
            val labels = firstFavorite?.key?.getLabels(global, localContext)
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
                        it.route == firstFavorite?.key?.route && it.stop == firstFavorite.key.stop
                    }
            ) {
                toastText =
                    labels?.let {
                        localContext.getString(
                            R.string.favorites_toast_add_multi,
                            it.route,
                            it.stop,
                        )
                    } ?: localContext.getString(R.string.favorites_toast_add_fallback)
            }

            goBack()

            toastText?.let {
                toastViewModel.showToast(
                    ToastViewModel.Toast(it, duration = ToastViewModel.Duration.Short)
                )
            }
        }
    }

    Column(Modifier.background(colorResource(R.color.fill2)).systemBarsPadding()) {
        Column(
            Modifier.background(colorResource(R.color.fill3))
                .padding(vertical = 16.dp)
                .padding(end = 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NavTextButton(
                    stringResource(R.string.cancel),
                    Modifier.padding(start = 6.dp),
                    colors =
                        buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = colorResource(R.color.key),
                        ),
                    action = goBack,
                )
                NavTextButton(stringResource(R.string.save), colors = ButtonDefaults.key()) {
                    updateCloseAndToast(mapOf(selectedRouteStopDirection to settings))
                }
            }
            Text(
                stringResource(
                    if (isFavorite) R.string.edit_favorite_title else R.string.add_favorite_title
                ),
                Modifier.padding(start = 16.dp),
                style = Typography.title1Bold,
            )
        }
        HaloSeparator()
        Column(
            Modifier.fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FavoriteStopCard(
                stop,
                lineOrRoute,
                stopDirections.find { it.id == selectedDirection } ?: stopDirections.first(),
                toggleDirection =
                    { selectedDirection = 1 - selectedDirection }.takeUnless {
                        stopDirections.size == 1 || isFavorite
                    },
                onlyServingOppositeDirection =
                    stopDirections.singleOrNull()?.id == 1 - initialDirection,
            )
            NotificationSettingsWidget(
                settings = settings.notifications,
                setSettings = { updatedSettings = settings.copy(notifications = it) },
            )
            if (isFavorite) {
                HaloSeparator()
                Button(
                    onClick = { updateCloseAndToast(mapOf(selectedRouteStopDirection to null)) },
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            colorResource(R.color.delete),
                            contentColor = colorResource(R.color.delete_background),
                        ),
                ) {
                    Text(
                        stringResource(R.string.remove_from_favorites),
                        style = Typography.bodySemibold,
                    )
                    Icon(painterResource(R.drawable.fa_delete), null)
                }
            }
        }
    }
}

@Preview(name = "Add Favorite")
@Composable
private fun SaveFavoritePagePreviewAdd() {
    val objects = TestData.clone()
    val koin = koinApplication {
        modules(
            module {
                single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
                single<IGlobalRepository> { MockGlobalRepository(GlobalResponse(objects)) }
                single {
                    FavoritesUsecases(
                        MockFavoritesRepository(),
                        MockSubscriptionsRepository(),
                        Dispatchers.IO,
                        MockAnalytics(),
                    )
                }
            }
        )
    }

    KoinIsolatedContext(koin) {
        MyApplicationTheme {
            SaveFavoritePage(
                routeId = Route.Id("Orange"),
                stopId = "place-welln",
                initialDirection = 0,
                context = EditFavoritesContext.StopDetails,
                goBack = {},
                toastViewModel = MockToastViewModel(),
            )
        }
    }
}

@Preview(name = "Edit Favorite")
@Composable
private fun SaveFavoritePagePreviewEdit() {
    val objects = TestData.clone()
    val koin = koinApplication {
        modules(
            module {
                single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
                single<IGlobalRepository> { MockGlobalRepository(GlobalResponse(objects)) }
                single {
                    FavoritesUsecases(
                        MockFavoritesRepository(
                            Favorites(
                                mapOf(
                                    RouteStopDirection(Route.Id("Orange"), "place-welln", 0) to
                                        FavoriteSettings(
                                            notifications =
                                                FavoriteSettings.Notifications(
                                                    enabled = true,
                                                    windows =
                                                        listOf(
                                                            FavoriteSettings.Notifications.Window(
                                                                startTime = LocalTime(8, 0),
                                                                endTime = LocalTime(9, 0),
                                                                setOf(DayOfWeek.FRIDAY),
                                                            )
                                                        ),
                                                )
                                        )
                                )
                            )
                        ),
                        MockSubscriptionsRepository(),
                        Dispatchers.IO,
                        MockAnalytics(),
                    )
                }
            }
        )
    }

    KoinIsolatedContext(koin) {
        MyApplicationTheme {
            SaveFavoritePage(
                routeId = Route.Id("Orange"),
                stopId = "place-welln",
                initialDirection = 0,
                context = EditFavoritesContext.StopDetails,
                goBack = {},
                toastViewModel = MockToastViewModel(),
            )
        }
    }
}
