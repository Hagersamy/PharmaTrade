import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

internal fun Project.kotlinMultiplatform(block: KotlinMultiplatformExtension.() -> Unit) =
    extensions.configure(KotlinMultiplatformExtension::class.java, block)

/** ":feature:auth:data" -> "com.pharmatrade.feature.auth.data" */
internal val Project.derivedNamespace: String
    get() = "com.pharmatrade" + path.replace(':', '.').replace('-', '_')

/** ":feature:auth:data" -> "feature-auth-data" — unique across all modules, unlike [Project.getName]. */
internal val Project.uniqueModuleName: String
    get() = path.removePrefix(":").replace(':', '-')
