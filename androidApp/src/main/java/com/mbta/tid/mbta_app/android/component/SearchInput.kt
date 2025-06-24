package com.mbta.tid.mbta_app.android.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mbta.tid.mbta_app.android.R
import com.mbta.tid.mbta_app.android.util.modifiers.haloContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    inputFieldFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onBarGloballyPositioned: (LayoutCoordinates) -> Unit = {},
    placeholder: @Composable () -> Unit,
) {
    val buttonColors =
        ButtonColors(
            containerColor = colorResource(R.color.fill3),
            disabledContainerColor = colorResource(R.color.fill3),
            contentColor = colorResource(R.color.deemphasized),
            disabledContentColor = colorResource(R.color.deemphasized),
        )

    val inputColors =
        SearchBarDefaults.inputFieldColors(
            focusedTextColor = colorResource(R.color.text),
            unfocusedTextColor = colorResource(R.color.text),
            focusedPlaceholderColor = colorResource(R.color.deemphasized),
            unfocusedPlaceholderColor = colorResource(R.color.deemphasized),
        )

    SearchInputField(
        colors = inputColors,
        query = query,
        placeholder = placeholder,
        expanded = expanded,
        onQueryChange = onQueryChange,
        onExpandedChange = onExpandedChange,
        modifier =
            modifier
                .heightIn(min = 44.dp)
                .padding(horizontal = 14.dp)
                .haloContainer(
                    2.dp,
                    borderRadius = 10.dp,
                    outlineColor =
                        if (expanded) colorResource(R.color.key_inverse).copy(alpha = 0.4f)
                        else colorResource(R.color.halo),
                    backgroundColor = colorResource(R.color.fill3),
                )
                .fillMaxWidth()
                .focusRequester(inputFieldFocusRequester)
                .onGloballyPositioned { layoutCoordinates ->
                    onBarGloballyPositioned(layoutCoordinates)
                },
        onSearch = {},
        leadingIcon = {
            Icon(
                painterResource(R.drawable.magnifying_glass),
                null,
                tint = colorResource(R.color.deemphasized),
            )
        },
        trailingIcon = {
            if (expanded) {
                Button(colors = buttonColors, onClick = { onExpandedChange(false) }) {
                    Icon(
                        painterResource(R.drawable.fa_xmark),
                        stringResource(R.string.close_button_label),
                        tint = colorResource(R.color.deemphasized),
                    )
                }
            }
        },
    )
}
