package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.component.HaloSeparator
import com.mbta.tid.mbta_app.android.component.NavTextButton
import com.mbta.tid.mbta_app.android.component.stopCard.FavoriteStopCard
import com.mbta.tid.mbta_app.android.favorites.NotificationSettingsWidget
import com.mbta.tid.mbta_app.android.state.getGlobalData
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fcmToken
import com.mbta.tid.mbta_app.android.util.getLabels
import com.mbta.tid.mbta_app.android.util.key
import com.mbta.tid.mbta_app.android.util.notificationPermissionState
import com.mbta.tid.mbta_app.android.util.stateJsonSaver
import com.mbta.tid.mbta_app.model.FavoriteSettings
import com.mbta.tid.mbta_app.model.Favorites
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteDetailsStopList
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.ErrorKey
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockFavoritesRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.repositories.MockSubscriptionsRepository
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.usecases.EditFavoritesContext
import com.mbta.tid.mbta_app.usecases.FavoritesUsecases
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.IFavoritesViewModel
import com.mbta.tid.mbta_app.viewModel.INotificationSettingsViewModel
import com.mbta.tid.mbta_app.viewModel.IToastViewModel
import com.mbta.tid.mbta_app.viewModel.MockToastViewModel
import com.mbta.tid.mbta_app.viewModel.ToastViewModel
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.koinInject
import org.koin.dsl.koinApplication
import org.koin.dsl.module

private sealed class SaveFavoriteMode {
    object Add : SaveFavoriteMode()
    object Edit : SaveFavoriteMode()
}

private typealias Add = SaveFavoriteMode.Add
private typealias Edit = SaveFavoriteMode.Edit

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SaveFavoritePage(
    routeId: LineOrRoute.Id,
    stopId: String,
    initialDirection: Int,
    context: EditFavoritesContext,
    goBack: () -> Unit,
    analytics: Analytics = koinInject(),
    favoritesViewModel: IFavoritesViewModel = koinInject(),
    notificationSettingsViewModel: INotificationSettingsViewModel = koinInject(),
    toastViewModel: IToastViewModel = koinInject(),
) {
    val resources = LocalResources.current

    val global = getGlobalData(ErrorKey(setOf(), "SaveFavoritePage"))
    val lineOrRoute = global?.getLineOrRoute(routeId)
    val stop = global?.getStop(stopId)

    if (global == null || lineOrRoute == null || stop == null) return

    val favoritesState by favoritesViewModel.models.collectAsState()
    val favorites = favoritesState.favorites

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

    val savedFavorite = favorites?.get(selectedRouteStopDirection)
    val saveMode = if (savedFavorite != null) Edit else Add
    var wasAdding by rememberSaveable { mutableStateOf(favorites != null && saveMode == Add) }
    val showDirectionToggle = stopDirections.size > 1 && wasAdding

    LaunchedEffect(saveMode, favorites) { if (favorites != null && saveMode == Add) wasAdding = true }

    val existingSettings = favorites?.get(selectedRouteStopDirection) ?: FavoriteSettings()
    var updatedSettings by
        rememberSaveable(saver = stateJsonSaver()) { mutableStateOf<FavoriteSettings?>(null) }
    val settings = updatedSettings ?: existingSettings

    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionState = notificationPermissionState { hasRequestedPermission = true }

    LaunchedEffect(notificationPermissionState.status) {
        if (notificationPermissionState.status.isGranted) {
            analytics.notificationsPermissionGranted()
        } else if (
            notificationPermissionState.status ==
                PermissionStatus.Denied(shouldShowRationale = true)
        ) {
            analytics.notificationsPermissionDenied()
        }
    }

    val notificationSettingsState by notificationSettingsViewModel.models.collectAsState()

    LaunchedEffect(existingSettings) {
        notificationSettingsViewModel.loadSavedSettings(existingSettings.notifications)
    }

    LaunchedEffect(notificationSettingsState) {
        val notificationSettings = notificationSettingsState.settings
        if (notificationSettings != null) {
            updatedSettings = settings.copy(notifications = notificationSettings)
        }
    }

    val includeAccessibility = SettingsCache.get(Settings.StationAccessibility)
    val notificationsEnabled = SettingsCache.get(Settings.Notifications)
    val currentLocale = stringResource(R.string.current_locale)

    fun updateFavorites(update: Map<RouteStopDirection, FavoriteSettings?>) {
        favoritesViewModel.updateFavorites(
            update,
            context,
            selectedDirection,
            fcmToken,
            currentLocale,
        )
        notificationSettingsViewModel.savedSettings()
    }

    val updateToastSingleText = stringResource(R.string.favorites_toast_add)
    val updateToastMultiText = stringResource(R.string.favorites_toast_add_multi)
    val updateToastFallbackText = stringResource(R.string.favorites_toast_add_fallback)

    fun updateCloseAndToast(update: Map<RouteStopDirection, FavoriteSettings?>) {
        updateFavorites(update)
        val favorited = update.filter { it.value != null }
        val firstFavorite = favorited.entries.firstOrNull()
        val labels = firstFavorite?.key?.getLabels(global, resources)
        var toastText: String? = null

        // If there's only a single favorite, show direction, route, and stop in the toast
        if (favorited.size == 1) {
            toastText =
                labels?.let { updateToastSingleText.format(it.direction, it.route, it.stop) }
                    ?: updateToastFallbackText
        }
        // If there are two favorites and they both have the same route and stop, omit direction
        else if (
            favorited.size == 2 &&
                favorited.keys.all {
                    it.route == firstFavorite?.key?.route && it.stop == firstFavorite.key.stop
                }
        ) {
            toastText =
                labels?.let { updateToastMultiText.format(it.route, it.stop) }
                    ?: updateToastFallbackText
        }

        goBack()

        toastText?.let {
            toastViewModel.showToast(
                ToastViewModel.Toast(it, duration = ToastViewModel.Duration.Short)
            )
        }
    }

    val removeToastText = stringResource(R.string.favorites_toast_remove)
    val removeToastFallbackText = stringResource(R.string.favorites_toast_remove_fallback)
    val removeToastUndoButtonText = stringResource(R.string.undo)

    fun removeCloseAndToast(removeFavorite: RouteStopDirection) {
        val deletedSettings = favorites?.get(removeFavorite) ?: FavoriteSettings()

        updateFavorites(mapOf(removeFavorite to null))

        val labels = removeFavorite.getLabels(global, resources)
        val toastText =
            labels?.let { removeToastText.format(it.direction, it.route, it.stop) }
                ?: removeToastFallbackText

        goBack()

        toastViewModel.showToast(
            ToastViewModel.Toast(
                toastText,
                duration = ToastViewModel.Duration.Short,
                action =
                    ToastViewModel.ToastAction.Custom(
                        actionLabel = removeToastUndoButtonText,
                        onAction = {
                            updateFavorites(mapOf(removeFavorite to deletedSettings))
                            toastViewModel.hideToast()
                        },
                    ),
            )
        )
    }

    Column {
        Column(
            Modifier
                .background(colorResource(R.color.fill3))
                .statusBarsPadding()
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
                    action = {
                        goBack()
                        notificationSettingsViewModel.loadSavedSettings(null)
                    },
                )
                NavTextButton(stringResource(R.string.save), colors = ButtonDefaults.key()) {
                    updateCloseAndToast(mapOf(selectedRouteStopDirection to settings))
                }
            }
            Text(
                stringResource(
                    if (saveMode == Edit) R.string.edit_favorite_title else R.string.add_favorite_title
                ),
                Modifier
                    .padding(start = 16.dp)
                    .semantics {
                        heading()
                    },
                style = Typography.title1Bold,
            )
        }
        HaloSeparator()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .background(colorResource(R.color.fill2))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .padding(bottom = 69.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FavoriteStopCard(
                    stop,
                    lineOrRoute,
                    stopDirections.find { it.id == selectedDirection } ?: stopDirections.first(),
                    toggleDirection =
                        {
                            selectedDirection = 1 - selectedDirection
                        }.takeIf { showDirectionToggle },
                    onlyServingOppositeDirection =
                        stopDirections.singleOrNull()?.id == 1 - initialDirection,
                )
                NotificationSettingsWidget(
                    viewModel = notificationSettingsViewModel,
                    showPresets = saveMode == Add || savedFavorite?.notifications?.enabled == false,
                    notificationPermissionState = notificationPermissionState,
                    hasRequestedPermission = hasRequestedPermission,
                )
            }
            if (saveMode == Edit) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.fill2).copy(alpha = 0.9f))
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { removeCloseAndToast(selectedRouteStopDirection) },
                        Modifier.fillMaxWidth().heightIn(min = 45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            buttonColors(
                                colorResource(R.color.delete),
                                contentColor = colorResource(R.color.delete_background),
                            ),
                    ) {
                        Text(
                            stringResource(R.string.remove_from_favorites),
                            Modifier.padding(end = 12.dp),
                            style = Typography.bodySemibold,
                        )
                        Icon(painterResource(R.drawable.fa_delete), null)
                    }
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
                        MockGlobalRepository(GlobalResponse(objects)),
                        MockSettingsRepository(),
                        MockSubscriptionsRepository(),
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
                        MockGlobalRepository(GlobalResponse(objects)),
                        MockSettingsRepository(),
                        MockSubscriptionsRepository(),
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
