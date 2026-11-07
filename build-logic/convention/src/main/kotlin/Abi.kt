import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.abi.tools.AbiFilters
import org.jetbrains.kotlin.abi.tools.AbiTools

/**
 * The public ABI of a module's classes, written as the Kotlin plugin's own dump format with its own engine
 * (`org.jetbrains.kotlin:abi-tools`). The Kotlin plugin's ABI validation reads no Android compilation, so the kit
 * runs the engine itself, the same way for every module.
 */
@CacheableTask
abstract class DumpAbi : DefaultTask() {
    /** Directories of class files: a JVM module's compile output. */
    @get:Classpath
    abstract val classes: ConfigurableFileCollection

    /** An Android variant's classes, which the Android Gradle plugin hands over as jars and directories. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dirs: ListProperty<Directory>

    @get:Classpath
    abstract val tools: ConfigurableFileCollection

    @get:Input
    abstract val excludedAnnotations: SetProperty<String>

    @get:OutputFile
    abstract val dump: RegularFileProperty

    @get:Inject
    abstract val workers: WorkerExecutor

    @TaskAction
    fun run() {
        val trees = classes.files + dirs.get().map { it.asFile }
        val jarFiles = jars.get().map { it.asFile }
        workers.classLoaderIsolation { classpath.from(tools) }.submit(DumpAbiWork::class.java) {
            // abi-tools reads class files and jars, not directories.
            files.from(trees.map { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "class" }.toList() }, jarFiles)
            excluded.set(excludedAnnotations)
            dump.set(this@DumpAbi.dump)
        }
    }
}

interface DumpAbiParameters : WorkParameters {
    val files: ConfigurableFileCollection
    val excluded: SetProperty<String>
    val dump: RegularFileProperty
}

abstract class DumpAbiWork : WorkAction<DumpAbiParameters> {
    override fun execute() {
        val filters = AbiFilters(emptySet(), emptySet(), emptySet(), parameters.excluded.get())
        val out = StringBuilder()
        AbiTools.getInstance(javaClass.classLoader).printJvmDump(out, parameters.files.files.sortedBy { it.path }, filters)
        parameters.dump.get().asFile.writeText(out.toString())
    }
}

/** Fails when the module's ABI differs from its reference dump in `api/`. */
@CacheableTask
abstract class CheckAbi : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val built: RegularFileProperty

    @get:Internal
    abstract val reference: RegularFileProperty

    /** The reference's text, so a missing reference is an input too. */
    @get:Input
    abstract val referenceText: Property<String>

    @get:Input
    abstract val updateTask: Property<String>

    @get:OutputFile
    abstract val stamp: RegularFileProperty

    @TaskAction
    fun run() {
        val reference = reference.get().asFile
        if (!reference.exists()) throw GradleException("${reference.path} is missing. Run ${updateTask.get()} and commit it.")
        if (referenceText.get() != built.get().asFile.readText()) {
            throw GradleException(
                "The public ABI changed. Run ${updateTask.get()}, review the diff of ${reference.path} and commit it. " +
                    "A removed or changed line breaks apps built against a released kit: see CLAUDE.md.",
            )
        }
        stamp.get().asFile.writeText("ok\n")
    }
}

/**
 * Fails when a line of the reference dump at the last release tag is gone: a class, a member or a signature that apps
 * built against that release may call. Default-argument bridges are left out: only the internal constructors have
 * default arguments, since the rules allow none in public API.
 */
abstract class CheckAbiCompatibility : DefaultTask() {
    @get:Internal
    abstract val repository: DirectoryProperty

    @get:Internal
    abstract val reference: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun run() {
        val root = repository.get().asFile
        val path = reference.get().asFile.relativeTo(root).invariantSeparatorsPath
        val tag = git(root, "describe", "--tags", "--abbrev=0", "--match", "v*")
            ?: return logger.lifecycle("No release tag yet: nothing to stay compatible with.")
        val released = git(root, "show", "$tag:$path")
            ?: return logger.lifecycle("$path is new since $tag.")
        val now = reference.get().asFile.takeIf { it.exists() }?.readText().orEmpty()
        val breaks = removedSince(parseDump(released), parseDump(now))
        if (breaks.isNotEmpty()) {
            throw GradleException("$path breaks apps built against $tag:\n" + breaks.joinToString("\n") { "  $it" })
        }
    }

    private fun git(root: File, vararg args: String): String? {
        val out = ByteArrayOutputStream()
        val result = exec.exec {
            workingDir = root
            commandLine(listOf("git") + args)
            standardOutput = out
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        return out.toString(Charsets.UTF_8).trim().takeIf { result.exitValue == 0 }
    }
}

/** Each class header of a dump with its member lines. */
private fun parseDump(text: String): Map<String, Set<String>> {
    val classes = LinkedHashMap<String, MutableSet<String>>()
    var current: MutableSet<String>? = null
    for (line in text.lines()) {
        when {
            line.endsWith("{") && !line.startsWith("\t") -> current = classes.getOrPut(line.removeSuffix("{").trim()) { LinkedHashSet() }
            line == "}" -> current = null
            line.isNotBlank() -> current?.add(line.trim())
        }
    }
    return classes
}

private fun removedSince(released: Map<String, Set<String>>, now: Map<String, Set<String>>): List<String> {
    val bridge = { line: String -> "DefaultConstructorMarker" in line || "\$default" in line }
    return released.flatMap { (header, members) ->
        val kept = now[header] ?: return@flatMap listOf("class gone or changed: $header")
        members.filterNot(bridge).filterNot(kept::contains).map { "$header: member gone or changed: $it" }
    }
}

/**
 * Registers dumpAbi, checkAbi, updateAbi and checkAbiCompatibility for this module, and makes `check` run both checks.
 * Returns the dump task, for the caller to feed its classes.
 */
internal fun Project.abiTasks(): TaskProvider<DumpAbi> {
    val tools = configurations.create("abiTools") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies { "abiTools"("org.jetbrains.kotlin:abi-tools:${libs.findVersion("kotlin").get().requiredVersion}") }

    val reference = layout.projectDirectory.file("api/$name.api")
    val update = "$path:updateAbi"
    val dump = tasks.register<DumpAbi>("dumpAbi") {
        this.tools.from(tools)
        excludedAnnotations.add(KIT_INTERNAL)
        this.dump.set(layout.buildDirectory.file("abi/$name.api"))
    }
    tasks.register("updateAbi") {
        val built = dump.flatMap { it.dump }
        val target = reference.asFile
        inputs.file(built)
        outputs.file(target)
        doLast { built.get().asFile.copyTo(target, overwrite = true) }
    }
    val checkAbi = tasks.register<CheckAbi>("checkAbi") {
        built.set(dump.flatMap { it.dump })
        this.reference.set(reference)
        referenceText.set(providers.fileContents(reference).asText.orElse(""))
        updateTask.set(update)
        stamp.set(layout.buildDirectory.file("abi/checked"))
    }
    val compatibility = tasks.register<CheckAbiCompatibility>("checkAbiCompatibility") {
        repository.set(rootProject.layout.projectDirectory)
        this.reference.set(reference)
    }
    tasks.named("check") { dependsOn(checkAbi, compatibility) }
    return dump
}
