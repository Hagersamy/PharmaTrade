import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Adds Compose Multiplatform (runtime, foundation, material3, icons, ui) to a KMP library. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("pharmatrade.kmp.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("org.jetbrains.compose")

        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.lib("compose-mp-runtime"))
                implementation(libs.lib("compose-mp-foundation"))
                implementation(libs.lib("compose-mp-material3"))
                implementation(libs.lib("compose-mp-material-icons-extended"))
                implementation(libs.lib("compose-mp-ui"))
            }
            sourceSets.getByName("androidMain").dependencies {
                implementation(project.dependencies.platform(libs.lib("androidx-compose-bom")))
            }
        }

        extensions.configure<LibraryExtension> {
            buildFeatures.compose = true
        }
    }
}
