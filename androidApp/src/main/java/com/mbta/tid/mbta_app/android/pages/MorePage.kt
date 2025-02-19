package com.mbta.tid.mbta_app.android.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.BuildConfig
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.more.MoreSectionView
import com.mbta.tid.mbta_app.android.more.MoreViewModel
import com.mbta.tid.mbta_app.android.util.Typography
import org.koin.compose.koinInject

@Composable
fun MorePage(
    bottomBar: @Composable () -> Unit,
    viewModel: MoreViewModel = MoreViewModel(LocalContext.current, koinInject())
) {

    val sections by viewModel.sections.collectAsState()
    Scaffold(bottomBar = bottomBar) { outerSheetPadding ->
        Column(Modifier.padding(outerSheetPadding).background(colorResource(R.color.fill3))) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(text = stringResource(R.string.more_title), style = Typography.title1Bold)
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

                    Text(stringResource(R.string.more_page_footer), style = Typography.callout)
                }
            }
        }
    }
}
