package com.mbta.tid.mbta_app.android

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher

fun hasClickActionLabel(expected: String?) =
    SemanticsMatcher("has click action label $expected") { node ->
        node.config[SemanticsActions.OnClick].label == expected
    }
