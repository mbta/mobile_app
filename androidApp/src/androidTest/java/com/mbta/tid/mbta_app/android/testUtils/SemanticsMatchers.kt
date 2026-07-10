package com.mbta.tid.mbta_app.android.testUtils

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

fun hasClickActionLabel(expected: String?) =
    SemanticsMatcher("has click action label $expected") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == expected
    }

fun hasTextMatching(regex: Regex): SemanticsMatcher {
    val propertyName = "${SemanticsProperties.Text.name} + ${SemanticsProperties.EditableText.name}"
    return SemanticsMatcher("$propertyName matches $regex") {
        val isInEditableTextValue =
            it.config.getOrNull(SemanticsProperties.EditableText)?.text?.matches(regex) ?: false
        val isInTextValue =
            it.config.getOrNull(SemanticsProperties.Text)?.any { item -> item.text.matches(regex) }
                ?: false
        isInEditableTextValue || isInTextValue
    }
}

fun hasContentDescriptionMatching(regex: Regex): SemanticsMatcher {
    val propertyName = SemanticsProperties.ContentDescription.name
    return SemanticsMatcher("$propertyName matches $regex") {
        it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { item ->
            item.matches(regex)
        } ?: false
    }
}

fun hasRole(role: Role) =
    SemanticsMatcher("has role $role") { node ->
        node.config.getOrNull(SemanticsProperties.Role) == role
    }
