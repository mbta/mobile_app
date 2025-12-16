package com.mbta.tid.mbta_app.android.pages

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mbta.tid.mbta_app.android.util.SettingsCache
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.fcmToken
import com.mbta.tid.mbta_app.android.util.key
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer
import com.mbta.tid.mbta_app.model.Dependency
import com.mbta.tid.mbta_app.model.getAllDependencies
import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.viewModel.MoreViewModel
import org.koin.compose.koinInject

@SuppressLint("LocalContextConfigurationRead")
@Composable
fun MorePage(bottomBar: @Composable () -> Unit, viewModel: MoreViewModel = koinInject()) {
    val context = LocalContext.current
    val locales = AppCompatDelegate.getApplicationLocales()
    val primaryLocale = locales[0] ?: context.resources.configuration.locales[0]
    val translation =
        when {
            primaryLocale.language == "es" -> "es"
            primaryLocale.language == "fr" -> "fr"
            primaryLocale.language == "ht" -> "ht"
            primaryLocale.language == "pt" -> "pt-BR"
            primaryLocale.language == "vi" -> "vi"
            primaryLocale.language == "zh" && primaryLocale.script == "Hans" -> "zh-Hans-CN"
            primaryLocale.language == "zh" && primaryLocale.script == "Hant" -> "zh-Hant-TW"
            else -> "en"
        }

    val navController = rememberNavController()

    val sections = remember {
        viewModel.getSections(translation, BuildConfig.VERSION_NAME) {
            navController.navigate("licenses")
        }
    }
    val dependencies = Dependency.getAllDependencies()
    var showingBuildNumber by remember { mutableStateOf(false) }
    val notificationsEnabled = SettingsCache.get(Settings.Notifications)

    Scaffold(
        Modifier.fillMaxSize().background(colorResource(R.color.fill3)),
        bottomBar = bottomBar,
    ) { outerSheetPadding ->
        Column(Modifier.padding(outerSheetPadding)) {
            NavHost(navController, startDestination = "more") {
                composable("more") {
                    Column {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text(
                                text = stringResource(R.string.more_title),
                                style = Typography.title1Bold,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                if (showingBuildNumber)
                                    stringResource(
                                        R.string.app_version_and_build,
                                        BuildConfig.VERSION_NAME,
                                        BuildConfig.VERSION_CODE,
                                    )
                                else
                                    stringResource(
                                        R.string.app_version_number,
                                        BuildConfig.VERSION_NAME,
                                    ),
                                if (!showingBuildNumber)
                                    Modifier.clickable { showingBuildNumber = true }
                                else Modifier,
                                style = Typography.footnote,
                            )
                        }
                        HorizontalDivider()
                        Column(
                            Modifier.verticalScroll(rememberScrollState())
                                .background(colorResource(R.color.fill1))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            sections.map { section ->
                                MoreSectionView(
                                    section = section,
                                    updateAccessibility = { includeAccessibility ->
                                        if (notificationsEnabled) {
                                            fcmToken?.let {
                                                viewModel.updateAccessibility(
                                                    it,
                                                    includeAccessibility,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painterResource(R.drawable.mbta_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Unspecified,
                                )

                                Text(
                                    stringResource(R.string.more_page_footer),
                                    style = Typography.callout,
                                )
                            }
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
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                colors = ButtonDefaults.key(),
                                onClick = { navController.popBackStack() },
                            ) {
                                Icon(
                                    painterResource(R.drawable.fa_chevron_left),
                                    modifier = Modifier.size(14.dp),
                                    contentDescription = null,
                                )
                                Text(
                                    stringResource(R.string.back_button_label),
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                            Text(
                                text = stringResource(R.string.software_licenses),
                                style = Typography.headlineSemibold,
                                modifier =
                                    Modifier.weight(1f).padding(start = 8.dp).semantics {
                                        heading()
                                    },
                            )
                        }
                        HorizontalDivider()
                        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                            Column(modifier = Modifier.haloContainer(1.dp)) {
                                dependencies.mapIndexed { index, it ->
                                    MoreButton(
                                        label = it.name,
                                        action = { navController.navigate("dependency/${index}") },
                                        icon = {
                                            Icon(
                                                painterResource(R.drawable.fa_chevron_right),
                                                modifier = Modifier.size(14.dp),
                                                contentDescription = null,
                                            )
                                        },
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
                    Column(Modifier.background(colorResource(R.color.fill1))) {
                        Row(
                            modifier =
                                Modifier.background(colorResource(R.color.fill3))
                                    .padding(vertical = 8.dp, horizontal = 10.dp)
                                    .fillMaxWidth()
                                    .height(44.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                colors = ButtonDefaults.key(),
                                onClick = { navController.popBackStack() },
                            ) {
                                Icon(
                                    painterResource(R.drawable.fa_chevron_left),
                                    modifier = Modifier.size(14.dp),
                                    contentDescription = null,
                                )
                                Text(
                                    stringResource(R.string.software_licenses),
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                        HorizontalDivider()
                        Column(
                            modifier =
                                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = dependency.name,
                                style = Typography.title2Bold,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(text = dependency.licenseText, style = Typography.body)
                        }
                    }
                }
            }
        }
    }
}
