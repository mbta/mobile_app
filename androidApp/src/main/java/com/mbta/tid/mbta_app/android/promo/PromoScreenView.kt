package com.mbta.tid.mbta_app.android.promo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.onboarding.OnboardingContentColumn
import com.mbta.tid.mbta_app.android.onboarding.OnboardingPieces
import com.mbta.tid.mbta_app.android.onboarding.PromoImage
import com.mbta.tid.mbta_app.model.FeaturePromo

@Composable
fun PromoScreenView(screen: FeaturePromo, onAdvance: () -> Unit) {

    val textScale = with(LocalDensity.current) { 1.sp.toPx() / 1.dp.toPx() }

    Column {
        when (screen) {
            FeaturePromo.CombinedStopAndTrip -> Column { LaunchedEffect(Unit) { onAdvance() } }
            FeaturePromo.EnhancedFavorites -> EnhancedFavorites(textScale, onAdvance)
        }
    }
}

@Composable
fun EnhancedFavorites(textScale: Float, onAdvance: () -> Unit) {
    OnboardingPieces.PageBox(
        colorResource(if (isSystemInDarkTheme()) R.color.fill1 else R.color.fill2)
    ) {
        if (textScale < 1.9f) {
            this@PageBox.PromoImage(R.drawable.feature_promo_favorites)
        }
        OnboardingContentColumn {
            OnboardingPieces.PageDescription(
                R.string.promo_favorites_header,
                R.string.promo_favorites_body,
                OnboardingPieces.Context.Promo,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OnboardingPieces.KeyButton(R.string.got_it, onClick = onAdvance)
        }
    }
}

@Preview(name = "Enhanced Favorites")
@Composable
private fun EnhancedFavoritesPreview() {
    PromoScreenView(FeaturePromo.EnhancedFavorites) {}
}
