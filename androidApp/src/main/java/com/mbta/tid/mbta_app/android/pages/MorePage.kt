package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mbta.tid.mbta_app.android.BuildConfig
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.more.MoreButton
import com.mbta.tid.mbta_app.android.more.MoreSectionView
import com.mbta.tid.mbta_app.android.more.MoreViewModel
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.model.Dependency
import com.mbta.tid.mbta_app.model.getAllDependencies
import org.koin.compose.koinInject

@Composable
fun MorePage(
    bottomBar: @Composable () -> Unit,
) {

    val navController = rememberNavController()
    val viewModel =
        MoreViewModel(LocalContext.current, { navController.navigate("licenses") }, koinInject())

    val sections by viewModel.sections.collectAsState()
    val dependencies = Dependency.getAllDependencies()

    Scaffold(bottomBar = bottomBar) { outerSheetPadding ->
        Column(Modifier.padding(outerSheetPadding).background(colorResource(R.color.fill3))) {
            NavHost(navController, startDestination = "more") {
                composable("more") {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = stringResource(R.string.more_title),
                            style = Typography.title1Bold,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            stringResource(R.string.app_version_number, BuildConfig.VERSION_NAME),
                            style = Typography.footnote
                        )
                    }
                    HorizontalDivider()
                    Column(
                        Modifier.verticalScroll(rememberScrollState())
                            .background(colorResource(R.color.fill1))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        sections.map { section ->
                            MoreSectionView(section = section) { setting ->
                                viewModel.toggleSetting(setting)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painterResource(R.drawable.mbta_logo),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Unspecified
                            )

                            Text(
                                stringResource(R.string.more_page_footer),
                                style = Typography.callout
                            )
                        }
                    }
                }
                composable("licenses") {
                    Column(Modifier.background(colorResource(R.color.fill1))) {
                        Row(
                            modifier =
                                Modifier.background(colorResource(R.color.fill3))
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                                    .fillMaxWidth()
                                    .height(44.dp),
                            horizontalArrangement = Arrangement.Absolute.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                colors =
                                    ButtonDefaults.buttonColors(
                                        contentColor = colorResource(R.color.key),
                                        containerColor = colorResource(R.color.fill3),
                                    ),
                                onClick = { navController.popBackStack() }
                            ) {
                                Icon(
                                    painterResource(R.drawable.fa_chevron_left),
                                    modifier = Modifier.size(14.dp),
                                    contentDescription = null
                                )
                                Text(
                                    stringResource(R.string.back_button_label),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.software_licenses),
                                style = Typography.headlineSemibold,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(Modifier.weight(1f))
                        }
                        HorizontalDivider(Modifier.padding(bottom = 24.dp))
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                            Column(
                                modifier =
                                    Modifier.border(
                                            1.dp,
                                            colorResource(R.color.halo),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clip(RoundedCornerShape(8.dp))
                            ) {
                                dependencies.mapIndexed { index, it ->
                                    MoreButton(
                                        label = it.name,
                                        action = { navController.navigate("dependency/${index}") },
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.fa_chevron_right),
                                                modifier = Modifier.size(14.dp),
                                                contentDescription = null
                                            )
                                        }
                                    )
                                    if (index < dependencies.size - 1) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
                composable("dependency/{index}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("index") ?: ""
                    val dependency =
                        Dependency.getAllDependencies().getOrNull(id.toInt()) ?: return@composable
                    Column(
                        Modifier.background(colorResource(R.color.fill1)),
                    ) {
                        Row(
                            modifier =
                                Modifier.background(colorResource(R.color.fill3))
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                                    .fillMaxWidth()
                                    .height(44.dp),
                            horizontalArrangement = Arrangement.Absolute.Left,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                colors =
                                    ButtonDefaults.buttonColors(
                                        contentColor = colorResource(R.color.key),
                                        containerColor = colorResource(R.color.fill3),
                                    ),
                                onClick = { navController.popBackStack() }
                            ) {
                                Icon(
                                    painterResource(R.drawable.fa_chevron_left),
                                    modifier = Modifier.size(14.dp),
                                    contentDescription = null
                                )
                                Text(
                                    stringResource(R.string.software_licenses),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(bottom = 24.dp))
                        Column(
                            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = dependency.name,
                                style = Typography.title2Bold,
                                modifier = Modifier.padding(bottom = 24.dp).semantics { heading() }
                            )
                            Text(text = dependency.licenseText, style = Typography.body)
                        }
                    }
                }
            }
        }
    }
}
