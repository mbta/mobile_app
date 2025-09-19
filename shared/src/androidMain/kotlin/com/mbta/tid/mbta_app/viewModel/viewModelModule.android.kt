package com.mbta.tid.mbta_app.viewModel

import org.koin.core.definition.Definition
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel as realViewModel
import org.koin.core.qualifier.Qualifier

internal actual inline fun <reified T : MoleculeScopeViewModel> Module.viewModel(
    qualifier: Qualifier?,
    noinline definition: Definition<T>,
): KoinDefinition<T> = realViewModel(qualifier, definition)
