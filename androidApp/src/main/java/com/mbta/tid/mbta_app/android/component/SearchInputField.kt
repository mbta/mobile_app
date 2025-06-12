package com.mbta.tid.mbta_app.android.component

/**
 * Derived from
 * https://cs.android.com/androidx/platform/frameworks/support/+/521f1b988fba92f8b79a43ac81dd177135294a34:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/SearchBar.kt;l=1297,
 * which is
 *
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
import android.annotation.SuppressLint
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.inputFieldColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.util.Typography
import com.mbta.tid.mbta_app.android.util.plus
import kotlinx.coroutines.delay

private const val AnimationDelayMillis: Int = 100 // MotionTokens.DurationShort2.toInt()

/** @see androidx.compose.material3.SearchBarDefaults.InputField */
@SuppressLint("PrivateResource")
@ExperimentalMaterial3Api
@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = inputFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val focused = interactionSource.collectIsFocusedAsState().value
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // These system resources replace internal string getters that were happening in the built in
    // composable, doing this runs the risk of needing to replace them if/when Android ever moves
    // their localizations around, but if they make that change, we should get compile errors on
    // the update, and can deal with it then.
    val searchSemantics = stringResource(androidx.compose.material3.R.string.m3c_search_bar_search)
    val suggestionsAvailableSemantics =
        stringResource(androidx.compose.material3.R.string.m3c_suggestions_available)

    fun TextFieldColors.textColor(enabled: Boolean, isError: Boolean, focused: Boolean): Color =
        when {
            !enabled -> disabledTextColor
            isError -> errorTextColor
            focused -> focusedTextColor
            else -> unfocusedTextColor
        }

    fun TextFieldColors.cursorColor(isError: Boolean): Color =
        if (isError) errorCursorColor else cursorColor

    val textColor =
        LocalTextStyle.current.color.takeOrElse {
            colors.textColor(enabled, isError = false, focused = focused)
        }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onExpandedChange(true) }
                .semantics {
                    contentDescription = searchSemantics
                    if (expanded) {
                        stateDescription = suggestionsAvailableSemantics
                    }
                    onClick {
                        focusRequester.requestFocus()
                        true
                    }
                },
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(Typography.callout).merge(color = textColor),
        cursorBrush = SolidColor(colors.cursorColor(isError = false)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        interactionSource = interactionSource,
        decorationBox =
            @Composable { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = query,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon?.let { leading -> { leading() } },
                    trailingIcon = trailingIcon?.let { trailing -> { trailing() } },
                    shape = SearchBarDefaults.inputFieldShape,
                    colors = colors,
                    contentPadding =
                        PaddingValues(vertical = 10.dp).plus(PaddingValues(start = 8.dp)),
                    container = {},
                )
            },
    )

    val shouldClearFocus = !expanded && focused
    LaunchedEffect(expanded) {
        if (shouldClearFocus) {
            // Not strictly needed according to the motion spec, but since the animation
            // already has a delay, this works around b/261632544.
            delay(AnimationDelayMillis.toLong())
            focusManager.clearFocus()
        }
    }
}
