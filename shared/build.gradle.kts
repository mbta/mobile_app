import co.touchlab.skie.configuration.DefaultArgumentInterop
import com.diffplug.spotless.FormatterFunc
import com.mbta.tid.mbta_app.gradle.CachedExecTask
import com.mbta.tid.mbta_app.gradle.CycloneDxBomTransformTask
import com.mbta.tid.mbta_app.gradle.DependencyCodegenTask
import com.mbta.tid.mbta_app.gradle.GithubLicenseResponse
import de.undercouch.gradle.tasks.download.Download
import java.io.ByteArrayOutputStream
import java.io.Serializable
import org.cyclonedx.model.AttachmentText
import org.cyclonedx.model.License
import org.cyclonedx.model.LicenseChoice
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.gradle.process.internal.ExecException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose)
    alias(libs.plugins.cycloneDx)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.sentry)
    alias(libs.plugins.serialization)
    alias(libs.plugins.skie)
    id("de.undercouch.download").version("5.6.0")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) } }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm { mainRun { mainClass.set("com.mbta.tid.mbta_app.ProjectUtilsKt") } }

    cocoapods {
        summary = "Common library for the MBTA mobile app"
        homepage = "https://github.com/mbta/mobile_app"
        license = "MIT"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "Shared"
            binaryOption("bundleId", "com.mbta.tid.mobileapp")
            export(libs.kotlinx.datetime)
            export(libs.sentry)
        }

        xcodeConfigurationToNativeBuildType["DevOrangeDebug"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["DevOrangeRelease"] = NativeBuildType.RELEASE
        xcodeConfigurationToNativeBuildType["StagingDebug"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["StagingRelease"] = NativeBuildType.RELEASE
        xcodeConfigurationToNativeBuildType["ProdDebug"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["ProdRelease"] = NativeBuildType.RELEASE
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.datetime)
                api(libs.sentry)
                api(libs.spatialk.geojson)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.androidx.datastore.preferences.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.encoding)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.molecule.runtime)
                implementation(libs.okio)
                implementation(libs.skie.configuration.annotations)
                implementation(libs.spatialk.turf)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.koin.test)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.okio.fakefilesystem)
                implementation(libs.turbine)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.lifecycle.viewmodel.android)
                implementation(libs.koin.androidxCompose)
                implementation(libs.ktor.client.okhttp)
            }
        }
        val iosMain by getting { dependencies { implementation(libs.ktor.client.darwin) } }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.kotlinpoet)
                implementation(libs.ktor.client.java)
            }
        }
    }
}

android {
    namespace = "com.mbta.tid.mbta_app"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint { disable.add("NullSafeMutableLiveData") }
}

skie {
    features {
        group { DefaultArgumentInterop.MaximumDefaultArgumentCount(8) }
        enableSwiftUIObservingPreview = true
    }
}

spotless {
    kotlin {
        custom(
            "ban getValue outside tests",
            object : Serializable, FormatterFunc.NeedsFile {
                override fun applyWithFile(text: String, file: File): String {
                    if (
                        file.path.contains("commonTest") ||
                            file.name == "ObjectCollectionBuilder.kt"
                    )
                        return text
                    val lines = text.lines()
                    for (line in lines.withIndex()) {
                        val column = line.value.indexOf("getValue(")
                        if (column != -1) {
                            throw IllegalStateException(
                                "${file.path}:${line.index + 1}:${column + 1} calls getValue"
                            )
                        }
                    }
                    return text
                }
            },
        )
    }
}

task("bom") { dependsOn("bomCodegenAndroid", "bomCodegenIos") }

if (DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX) {
    tasks.getByName("compileKotlinIosX64").dependsOn("bomCodegenIos")

    tasks.getByName("compileKotlinIosArm64").dependsOn("bomCodegenIos")

    tasks.getByName("compileKotlinIosSimulatorArm64").dependsOn("bomCodegenIos")

    tasks.findByName("compileIosMainKotlinMetadata")?.dependsOn("bomCodegenIos")
}

// don't run Android BOM codegen for iOS-only CI
if (
    !(System.getenv("CI")?.lowercase() == "true" &&
        DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX)
) {
    tasks.getByName("preBuild").dependsOn("bomCodegenAndroid")
}

tasks.withType<JavaExec> {
    // https://blog.thecodewhisperer.com/permalink/stdin-gradle-kotlin-dsl
    // getting the task by name was failing for some reason
    if (this.name == "jvmRun") {
        standardInput = System.`in`
        workingDir = projectDir.parentFile
    }
}

task<DependencyCodegenTask>("bomCodegenAndroid") {
    dependsOn("bomAndroid")
    mustRunAfter("spotlessKotlin", "cyclonedxBom")
    inputPath = layout.buildDirectory.file("boms/bom-android.json")
    outputPath =
        layout.projectDirectory.file(
            "src/androidMain/kotlin/com/mbta/tid/mbta_app/model/Dependency.android.kt"
        )
}

task<DependencyCodegenTask>("bomCodegenIos") {
    dependsOn("bomIos")
    inputPath = layout.buildDirectory.file("boms/bom-ios.xml")
    outputPath =
        layout.projectDirectory.file(
            "src/iosMain/kotlin/com/mbta/tid/mbta_app/model/Dependency.ios.kt"
        )
}

task<CycloneDxBomTransformTask>("bomAndroid") {
    dependsOn(":androidApp:cyclonedxBom")
    inputPath = projects.androidApp.dependencyProject.layout.buildDirectory.file("reports/bom.json")
    outputPath = layout.buildDirectory.file("boms/bom-android.json")
    transform = {
        components =
            components.filter { it.purl != "pkg:maven/MBTA_App/shared@unspecified?type=jar" }
    }
}

task<CycloneDxBomTransformTask>("bomIos") {
    dependsOn("bomIosMerged")
    inputPath = layout.buildDirectory.file("boms/bom-ios-merged.xml")
    outputPath = layout.buildDirectory.file("boms/bom-ios.xml")
    transform = {
        components =
            components.filterNot {
                setOf(
                        "pkg:maven/MBTA_App/shared@unspecified?type=jar",
                        "pkg:swift/iosApp.xcworkspace@latest",
                        "pkg:swift/iosApp@latest",
                    )
                    .contains(it.purl)
            }
        // the hardcoded stdlib bom doesn't handle this
        components
            .single { it.group == "org.jetbrains.kotlin" && it.name == "kotlin-stdlib" }
            .licenses
            .addLicense(
                License().apply {
                    name = "Apache Harmony copyright notice"
                    setLicenseText(
                        AttachmentText().apply {
                            // this MIME type is backwards but it is consistent with other tools
                            contentType = "plain/text"
                            text =
                                """
Apache Harmony
Copyright 2006, 2010 The Apache Software Foundation.

This product includes software developed at
The Apache Software Foundation (http://www.apache.org/).

Portions of Apache Harmony were originally developed by
Intel Corporation and are licensed to the Apache Software
Foundation under the "Software Grant and Corporate Contribution
License Agreement" and for which the following copyright notices
apply
         (C) Copyright 2005 Intel Corporation
         (C) Copyright 2005-2006 Intel Corporation
         (C) Copyright 2006 Intel Corporation
                                """
                                    .trim()
                        }
                    )
                }
            )
    }
}

task<CachedExecTask>("bomIosMerged") {
    dependsOn(
        "bomIosKotlinDeps",
        "bomIosKotlinStdlib",
        "bomIosCocoapods",
        "bomIosSwiftPM",
        "bomCycloneDxCliDownload",
    )
    inputFiles =
        layout.buildDirectory.files(
            "boms/bom-ios-kotlin-deps.xml",
            "boms/bom-ios-kotlin-stdlib.xml",
            "boms/bom-ios-cocoapods.xml",
            "boms/bom-ios-swiftpm.json",
        )
    outputFile = layout.buildDirectory.file("boms/bom-ios-merged.xml")
    workingDir = layout.buildDirectory.dir("boms")
    commandLine(
        layout.buildDirectory.file("boms/cyclonedx-cli").get().toString(),
        "merge",
        "--input-files",
        "bom-ios-kotlin-deps.xml",
        "bom-ios-kotlin-stdlib.xml",
        "bom-ios-cocoapods.xml",
        "bom-ios-swiftpm.json",
        "--output-file",
        "bom-ios-merged.xml",
    )
}

task<Download>("bomCycloneDxCliDownload") {
    val os =
        DefaultNativePlatform.getCurrentOperatingSystem().let {
            when {
                it.isLinux -> "linux"
                it.isMacOsX -> "osx"
                else -> throw IllegalStateException("can't download CycloneDX CLI for $it")
            }
        }
    val arch =
        DefaultNativePlatform.getCurrentArchitecture().let {
            when {
                it.isArm64 -> "arm64"
                it.isAmd64 -> "x64"
                else -> throw IllegalStateException("can't download CycloneDX CLI for $it")
            }
        }
    src("https://github.com/CycloneDX/cyclonedx-cli/releases/download/v0.27.1/cyclonedx-$os-$arch")
    dest(layout.buildDirectory.file("boms/cyclonedx-cli"))
    onlyIfModified(true)
    doLast {
        exec { commandLine("chmod", "+x", layout.buildDirectory.file("boms/cyclonedx-cli").get()) }
    }
}

task("bomIosKotlinDeps") {
    dependsOn(tasks.cyclonedxBom)
    mustRunAfter("bomCodegenAndroid")
}

task<Copy>("bomIosKotlinStdlib") {
    mustRunAfter("bomCodegenAndroid")
    from(layout.projectDirectory.file("src/iosMain/xml/bom-ios-kotlin-stdlib.xml"))
    into(layout.buildDirectory.dir("boms"))
}

task<CycloneDxBomTransformTask>("bomIosCocoapods") {
    dependsOn("bomIosCocoapodsRaw")
    mustRunAfter("bomIosKotlinDeps", "bomIosKotlinStdlib")
    inputPath = layout.buildDirectory.file("boms/bom-ios-cocoapods-raw.xml")
    outputPath = layout.buildDirectory.file("boms/bom-ios-cocoapods.xml")
    transform = {
        components =
            components.filter { it.purl != "pkg:cocoapods/shared@1.0?file_name=..%2Fshared%2F" }
        // cyclonedx-cocoapods doesn't embed licenses
        for (component in components) {
            // this is all very manual, so make sure it doesn't start silently failing
            check(component.name.startsWith("Sentry"))
            component.licenses.licenses
                .single()
                .setLicenseText(
                    AttachmentText().apply {
                        text =
                            layout.projectDirectory
                                .file("../iosApp/Pods/Sentry/LICENSE.md")
                                .asFile
                                .readText()
                    }
                )
        }
    }
}

task<CachedExecTask>("bomIosCocoapodsRaw") {
    inputFiles = layout.projectDirectory.files("../iosApp/Podfile.lock")
    outputFile = layout.buildDirectory.file("boms/bom-ios-cocoapods-raw.xml")
    workingDir = provider { layout.projectDirectory.dir("../iosApp") }
    commandLine("bundle", "exec", "cyclonedx-cocoapods", "-o", outputFile.get().toString())
    onError = { error ->
        throw IllegalStateException("\nerror: BOM generation failed, see Gotchas in README", error)
    }
}

task<CycloneDxBomTransformTask>("bomIosSwiftPM") {
    dependsOn("bomIosSwiftPMRaw")
    mustRunAfter("bomIosKotlinDeps", "bomIosKotlinStdlib")
    inputPath = layout.buildDirectory.file("boms/bom-ios-swiftpm-raw.json")
    outputPath = layout.buildDirectory.file("boms/bom-ios-swiftpm.json")
    transform = {
        // the cdxgen metadata gets emitted as the wrong kind of element after merging and breaks
        // the parser, or something like that
        metadata.tools = emptyList()
        // even with FETCH_LICENSES=true, the actual license text isn't embedded, so fetch it
        // directly
        for (component in components) {
            component.licenses = LicenseChoice()
            check(component.purl.startsWith("pkg:swift/github.com/")) {
                "bad purl ${component.purl}"
            }
            val licenseApiPath =
                component.purl
                    .replace("pkg:swift/github.com/", "/repos/")
                    .replace(
                        "@",
                        if (
                            component.name == "gtm-session-fetcher" ||
                                component.group == "github.com/mapbox"
                        )
                            "/license?ref=v"
                        else "/license?ref=",
                    )
            val ghOutput = ByteArrayOutputStream()
            try {
                exec {
                    commandLine("gh", "api", licenseApiPath)
                    standardOutput = ghOutput
                }
            } catch (e: ExecException) {
                throw IllegalStateException(
                    "\nerror: `gh api $licenseApiPath` failed, ${when {
                        DefaultNativePlatform.getCurrentOperatingSystem().isMacOsX -> "`brew install gh`"
                        else -> "install it"
                    }} and try `gh auth login`",
                    e,
                )
            }
            val apiResponse = ghOutput.toString()
            val licenseResponse = GithubLicenseResponse.decode(apiResponse)

            component.licenses.addLicense(
                License().apply {
                    setLicenseText(
                        AttachmentText().apply {
                            encoding = licenseResponse.encoding
                            check(encoding == "base64") {
                                "encoding $encoding not base64 as expected"
                            }
                            text = licenseResponse.content.replace(Regex("\\s+"), "")
                            val knownLicense = licenseResponse.license.spdxId
                            if (knownLicense != "NOASSERTION") {
                                id = knownLicense
                            }
                        }
                    )
                }
            )
        }
    }
}

task<CachedExecTask>("bomIosSwiftPMRaw") {
    inputFiles =
        layout.projectDirectory.files(
            "../iosApp/iosApp.xcworkspace/xcshareddata/swiftpm/Package.resolved"
        )
    outputFile = layout.buildDirectory.file("boms/bom-ios-swiftpm-raw.json")
    workingDir = provider { layout.projectDirectory.dir("../iosApp") }
    commandLine(
        "npx",
        "@cyclonedx/cdxgen",
        "-t",
        "swift",
        "-o",
        outputFile.get().toString(),
        "iosApp.xcworkspace",
    )
}

tasks.cyclonedxBom {
    includeConfigs =
        listOf(
            "commonMainImplementationDependenciesMetadata",
            "iosMainImplementationDependenciesMetadata",
        )
    destination = layout.buildDirectory.dir("boms").get().asFile
    outputName = "bom-ios-kotlin-deps"
    outputFormat = "xml"
    outputs.files(layout.buildDirectory.file("boms/bom-ios-kotlin-deps.xml"))
}

mokkery {
    ignoreInlineMembers.set(true)
    ignoreFinalMembers.set(true)
}

sentryKmp { autoInstall.commonMain.enabled = false }
