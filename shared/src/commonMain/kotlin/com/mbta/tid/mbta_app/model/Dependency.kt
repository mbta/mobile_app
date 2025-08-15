package com.mbta.tid.mbta_app.model

public data class Dependency(val id: String, val name: String, val licenseText: String) {
    public companion object
}

public expect fun Dependency.Companion.getAllDependencies(): List<Dependency>
