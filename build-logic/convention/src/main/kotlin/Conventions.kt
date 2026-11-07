import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinTopLevelExtension

/** The root catalog, so a convention plugin reads the same versions every module does. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.int(alias: String): Int =
    findVersion(alias).orElseThrow { IllegalStateException("libs.versions.toml has no version '$alias'") }
        .requiredVersion
        .toInt()

/** The opt-in marker for declarations only Visor and the kit's own modules may use. */
internal const val KIT_INTERNAL = "com.macedonga.visor.kit.protocol.KitInternal"

/**
 * The rules every published module follows: explicit API mode, and an ABI dump in `api/` that `check` compares the
 * build against. Declarations marked [KIT_INTERNAL] stay out of the dump: they carry no compatibility promise.
 * Returns the dump task, for the module type's convention to feed its classes.
 */
internal fun Project.publicApiRules(): TaskProvider<DumpAbi> {
    (extensions.getByName("kotlin") as KotlinTopLevelExtension).explicitApi()
    return abiTasks()
}
