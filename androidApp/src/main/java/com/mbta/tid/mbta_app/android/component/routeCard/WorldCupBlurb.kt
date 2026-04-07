package com.mbta.tid.mbta_app.android.component.routeCard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.mbta.tid.mbta_app.analytics.Analytics
import com.mbta.tid.mbta_app.analytics.MockAnalytics
import com.mbta.tid.mbta_app.android.MyApplicationTheme
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.stopDetails.StopDetailsFilteredDeparturesView
import com.mbta.tid.mbta_app.android.stopDetails.TripRouteAccents
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.LineOrRoute
import com.mbta.tid.mbta_app.model.RouteCardData
import com.mbta.tid.mbta_app.model.StopDetailsFilter
import com.mbta.tid.mbta_app.model.WorldCupService
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.repositories.IErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.IGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockErrorBannerStateRepository
import com.mbta.tid.mbta_app.repositories.MockGlobalRepository
import com.mbta.tid.mbta_app.repositories.MockSettingsRepository
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import com.mbta.tid.mbta_app.utils.TestData
import com.mbta.tid.mbta_app.viewModel.IStopDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.MockStopDetailsViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

@Composable
fun WorldCupBlurb(leaf: RouteCardData.Leaf, routeAccents: TripRouteAccents, offerDetails: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.soccer_ball_icon), contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    AnnotatedString.fromHtml(
                        if (leaf.directionId == 0)
                            stringResource(R.string.world_cup_service_outbound)
                        else stringResource(R.string.world_cup_service_inbound)
                    ),
                    color = colorResource(R.color.text),
                    lineHeight = 24.sp,
                    style = Typography.body,
                )
                Text(
                    stringResource(R.string.world_cup_ticket_required),
                    Modifier.alpha(0.6f),
                    color = colorResource(R.color.text),
                    style = Typography.footnote,
                )
            }
        }
        if (offerDetails) {
            val context = LocalContext.current
            TextButton(
                onClick = {
                    val webpage: Uri = WorldCupService.scheduleUrl.toUri()
                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Log.i("WorldCupBlurb", "Failed to navigate to link on WorldCupBlurb click")
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonColors(
                        routeAccents.color,
                        routeAccents.color,
                        routeAccents.color,
                        routeAccents.color,
                    ),
                modifier = Modifier.heightIn(min = 44.dp).fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.view_details),
                    color = routeAccents.textColor,
                    modifier = Modifier.padding(4.dp),
                    style = Typography.bodySemibold,
                )
            }
        }
    }
}

@Preview
@Composable
private fun WorldCupBlurbNearbyPreview() {
    val objects = TestData.clone("WorldCupBlurbPreview")
    objects.put(WorldCupService.route)
    val route = WorldCupService.route
    val stop = objects.getStop("place-sstat")
    val global = GlobalResponse(objects)
    val data =
        RouteCardData(
            LineOrRoute.Route(route),
            listOf(
                RouteCardData.RouteStopData(
                    route,
                    stop,
                    listOf(
                        RouteCardData.Leaf(
                            LineOrRoute.Route(route),
                            stop,
                            directionId = 0,
                            routePatterns = emptyList(),
                            stopIds = emptySet(),
                            upcomingTrips = emptyList(),
                            alertsHere = emptyList(),
                            allDataLoaded = true,
                            hasSchedulesToday = true,
                            subwayServiceStartTime = null,
                            alertsDownstream = emptyList(),
                            RouteCardData.Context.NearbyTransit,
                        )
                    ),
                    global,
                )
            ),
            EasternTimeInstant.now(),
        )
    stopKoin()
    startKoin {
        modules(
            module {
                single<Analytics> { MockAnalytics() }
                single<SettingsCache> { SettingsCache(MockSettingsRepository()) }
            }
        )
    }
    MyApplicationTheme {
        RouteCard(data, global, EasternTimeInstant.now(), { null }, false, { _, _ -> })
    }
}

@Preview
@Composable
private fun WorldCupBlurbStopDetailsPreview() {
    val objects = TestData.clone("WorldCupBlurbPreview")
    objects.put(WorldCupService.route)
    val route = WorldCupService.route
    val stop = objects.getStop("place-sstat")
    val global = GlobalResponse(objects)
    val data =
        RouteCardData(
            LineOrRoute.Route(route),
            listOf(
                RouteCardData.RouteStopData(
                    route,
                    stop,
                    listOf(
                        RouteCardData.Leaf(
                            LineOrRoute.Route(route),
                            stop,
                            directionId = 0,
                            routePatterns = emptyList(),
                            stopIds = emptySet(),
                            upcomingTrips = emptyList(),
                            alertsHere = emptyList(),
                            allDataLoaded = true,
                            hasSchedulesToday = true,
                            subwayServiceStartTime = null,
                            alertsDownstream = emptyList(),
                            RouteCardData.Context.NearbyTransit,
                        )
                    ),
                    global,
                )
            ),
            EasternTimeInstant.now(),
        )
    stopKoin()
    startKoin {
        modules(
            module {
                single<Analytics> { MockAnalytics() }
                single<IErrorBannerStateRepository> { MockErrorBannerStateRepository() }
                single<IGlobalRepository> { MockGlobalRepository(GlobalResponse(objects)) }
                single<IStopDetailsViewModel> { MockStopDetailsViewModel() }
                single<SettingsCache> { SettingsCache(MockSettingsRepository()) }
            }
        )
    }
    MyApplicationTheme {
        StopDetailsFilteredDeparturesView(
            stop.id,
            StopDetailsFilter(route.id, 0),
            tripFilter = null,
            leaf = data.stopData.single().data.single(),
            selectedDirection = data.stopData.single().directions.first(),
            allAlerts = null,
            now = EasternTimeInstant.now(),
            updateTripFilter = {},
            tileScrollState = rememberScrollState(),
            isFavorite = false,
            openModal = {},
            openSheetRoute = {},
        )
    }
}
