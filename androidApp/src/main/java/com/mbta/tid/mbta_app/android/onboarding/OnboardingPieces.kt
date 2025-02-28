package com.mbta.tid.mbta_app.android.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.Typography

object OnboardingPieces {
    @Composable
    fun PageDescription(
        @StringRes headerId: Int,
        @StringRes bodyId: Int,
        modifier: Modifier = Modifier
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(headerId),
                modifier = Modifier.semantics { heading() },
                style = Typography.title1Bold,
            )
            Text(stringResource(bodyId), style = Typography.title3)
        }
    }

    @Composable
    fun PageBox(backgroundColor: Color, content: @Composable BoxScope.() -> Unit) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundColor), content = content)
    }

    @Composable
    fun PageBox(backgroundImage: Painter, content: @Composable BoxScope.() -> Unit) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .paint(
                        painter = backgroundImage,
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop
                    ),
            content = content
        )
    }

    private val buttonModifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)

    @Composable
    fun KeyButton(@StringRes textId: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
        Button(
            modifier = buttonModifier.then(modifier),
            shape = RoundedCornerShape(8.dp),
            onClick = onClick,
            colors =
                buttonColors(
                    containerColor = colorResource(R.color.key),
                    contentColor = colorResource(R.color.fill3)
                )
        ) {
            Text(
                stringResource(textId),
                textAlign = TextAlign.Center,
                style = Typography.bodySemibold,
            )
        }
    }

    @Composable
    fun SecondaryButton(
        @StringRes textId: Int,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Button(
            modifier =
                buttonModifier
                    .then(modifier)
                    .border(
                        1.dp,
                        color = colorResource(R.color.key),
                        shape = RoundedCornerShape(8.dp)
                    ),
            shape = RoundedCornerShape(8.dp),
            onClick = onClick,
            colors =
                buttonColors(
                    containerColor = colorResource(R.color.fill1),
                    contentColor = colorResource(R.color.key)
                )
        ) {
            Text(stringResource(textId), textAlign = TextAlign.Center, style = Typography.body)
        }
    }
}

@Composable
fun BoxScope.OnboardingImage(@DrawableRes drawableId: Int, size: Dp?, offsetY: Dp = 0.dp) {
    Image(
        painterResource(drawableId),
        contentDescription = null,
        modifier =
            Modifier.align(Alignment.Center)
                .offset(y = offsetY)
                .then(if (size != null) Modifier.size(size) else Modifier.fillMaxSize()),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun BoxScope.OnboardingContentColumn(content: @Composable ColumnScope.() -> Unit) {
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp
    val bottomPadding = if (screenHeight < 812) 16.dp else 52.dp
    Column(
        modifier =
            Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp)
                .padding(top = 16.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}
