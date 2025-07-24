package com.mbta.tid.mbta_app.android.promo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.mbta.tid.mbta_app.model.FeaturePromo

@Composable
fun PromoPage(screens: List<FeaturePromo>, onFinish: () -> Unit, onAdvance: () -> Unit = {}) {

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    PromoScreenView(screens[selectedIndex]) {
        if (selectedIndex < screens.size - 1) {
            selectedIndex += 1
            onAdvance()
        } else {
            onFinish()
        }
    }
}

@Preview
@Composable
private fun AllPromos() {
    PromoPage(listOf(FeaturePromo.EnhancedFavorites), {}) {}
}
