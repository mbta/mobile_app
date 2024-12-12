package com.mbta.tid.mbta_app.android.search

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mbta.tid.mbta_app.android.R

@ExperimentalMaterial3Api
@Composable
fun SearchBarOverlay(content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val searchInputState = rememberTextFieldState()
    Box(contentAlignment = Alignment.TopCenter) {
        Box(
            modifier =
                Modifier.absoluteOffset {
                        if (expanded) IntOffset(0, 0) else IntOffset(0, 12.dp.roundToPx())
                    }
                    .zIndex(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier.absoluteOffset(y = 3.5.dp)
                        .height(64.dp)
                        .width(364.dp)
                        .border(2.dp, colorResource(R.color.halo), RoundedCornerShape(10.dp))
            )
            SearchBar(
                shape = RoundedCornerShape(10.dp),
                colors = SearchBarDefaults.colors(containerColor = colorResource(R.color.fill3)),
                inputField = {
                    SearchBarDefaults.InputField(
                        colors =
                            SearchBarDefaults.inputFieldColors(
                                focusedTextColor = colorResource(R.color.deemphasized),
                                unfocusedTextColor = colorResource(R.color.deemphasized),
                                focusedPlaceholderColor = colorResource(R.color.deemphasized),
                                unfocusedPlaceholderColor = colorResource(R.color.deemphasized),
                            ),
                        query = searchInputState.text.toString(),
                        placeholder = { Text("Stops") },
                        expanded = expanded,
                        onQueryChange = { query ->
                            searchInputState.edit {
                                delete(0, length)
                                append(query)
                                val queryChars = query.toCharArray()
                                val chars = asCharSequence()
                                if (queryChars.isNotEmpty()) {
                                    if (chars.isEmpty()) {
                                        append(queryChars.joinToString(""))
                                    } else if (chars.length > queryChars.size) {
                                        delete(queryChars.size, chars.length)
                                    } else {
                                        for (i in queryChars.indices) {
                                            if (i > chars.length) {
                                                append(
                                                    queryChars
                                                        .slice(IntRange(i, queryChars.size - 1))
                                                        .joinToString("")
                                                )
                                                break
                                            } else if (queryChars[i] != chars[i]) {
                                                delete(i, chars.length)
                                                append(
                                                    queryChars
                                                        .slice(IntRange(i, queryChars.size - 1))
                                                        .joinToString("")
                                                )
                                                break
                                            }
                                        }
                                    }
                                } else {
                                    delete(0, length)
                                }
                            }
                        },
                        onExpandedChange = { expanded = it },
                        onSearch = { expanded = false },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.magnifying_glass),
                                "Search",
                                tint = colorResource(R.color.deemphasized)
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painterResource(R.drawable.microphone),
                                "Voice Search",
                                tint = colorResource(R.color.deemphasized)
                            )
                        }
                    )
                },
                expanded = expanded,
                onExpandedChange = {},
            ) {}
        }
        content()
    }
}
