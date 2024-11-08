package com.mbta.tid.mbta_app.model

data class Dependency(val id: String, val name: String, val licenseText: String) {
    companion object
}

expect fun Dependency.Companion.getAllDependencies(): List<Dependency>
