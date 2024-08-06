plugins {
    id("java")
    kotlin("jvm")
}

repositories { mavenCentral() }

kotlin { jvmToolchain(17) }

dependencies { implementation(libs.appium.javaClient) }

abstract class IntegrationTestExec : JavaExec() {
    override fun exec() {
        classpath =
            project.sourceSets.main.get().runtimeClasspath + project.sourceSets.main.get().output

        environment(
            "APPIUM_PATH",
            project.projectDir.resolve("node_modules/appium/build/lib/main.js")
        )

        super.exec()
    }
}

tasks.register<IntegrationTestExec>("runIntegrationTestAndroid") {
    dependsOn(tasks.compileJava)
    dependsOn(projects.androidApp.dependencyProject.tasks["assembleDebug"])

    mainClass = "com.mbta.tid.mbta_app.integrationTests.AndroidHost"

    args(
        projects.androidApp.dependencyProject.layout.buildDirectory
            .file("outputs/apk/debug/androidApp-debug.apk")
            .get()
            .toString()
    )
}

val iosExportTarget = layout.buildDirectory.file("integrationTestBuild.xcarchive")

tasks.register<Exec>("iosExport") {
    workingDir = rootDir.resolve("iosApp")
    commandLine(
        "xcodebuild",
        "-quiet",
        "-workspace",
        "iosApp.xcworkspace",
        "-scheme",
        "Staging",
        "archive",
        "-destination",
        "variant=iphonesimulator",
        "-archivePath",
        iosExportTarget.get().toString()
    )
}

tasks.register<IntegrationTestExec>("runIntegrationTestIos") {
    dependsOn(tasks.compileJava)
    dependsOn("iosExport")

    mainClass = "com.mbta.tid.mbta_app.integrationTests.IosHost"

    args(iosExportTarget.get().asFile.resolve("Products/Applications/iosApp.app").absolutePath)
}
