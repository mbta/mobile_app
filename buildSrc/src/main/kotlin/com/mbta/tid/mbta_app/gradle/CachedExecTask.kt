package com.mbta.tid.mbta_app.gradle

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class CachedExecTask @Inject constructor(private val exec: ExecOperations) :
    DefaultTask() {
    @get:InputFiles abstract var inputFiles: FileCollection
    @get:OutputFile abstract var outputFile: Provider<RegularFile>
    @get:Internal abstract var workingDir: Provider<Directory>
    @get:Input abstract var commandLine: List<String>

    fun commandLine(vararg arg: String) {
        commandLine = arg.asList()
    }

    @TaskAction
    fun run() {
        exec.exec {
            workingDir(this@CachedExecTask.workingDir)
            commandLine(this@CachedExecTask.commandLine)
        }.assertNormalExitValue()
    }
}