import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Base for every shared module: a Kotlin Multiplatform library targeting Android, desktop (JVM)
 * and iOS, with an Android namespace derived from the Gradle path.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.library")

        kotlinMultiplatform {
            jvmToolchain(11)
            androidTarget()
            jvm("desktop")
            iosArm64()
            iosSimulatorArm64()

            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.lib("kotlinx-coroutines-core"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
            }
        }

        extensions.configure<LibraryExtension> {
            namespace = derivedNamespace
            compileSdk = 36
            defaultConfig.minSdk = 24
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
        }

        // Many modules share a leaf name ("data", "domain", ...); give each a unique Kotlin module
        // name so their META-INF/*.kotlin_module files don't collide when packaged into one APK/JAR.
        val moduleName = uniqueModuleName
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions.moduleName.set(moduleName)
        }
    }
}
