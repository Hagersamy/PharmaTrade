import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Dependency rules (enforced by what these plugins wire up):
 *  - domain       -> :core:common
 *  - data         -> own :domain, :core:common, :core:network
 *  - presentation -> own :domain, :core:common, :core:ui
 * A feature may additionally depend on *another feature's :domain* (declared explicitly in its
 * build file), but never on another feature's :data or :presentation.
 */
class FeatureDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("pharmatrade.kmp.library")

        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                api(project(":core:common"))
                implementation(libs.lib("kotlinx-datetime"))
            }
        }
    }
}

class FeatureDataConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("pharmatrade.kmp.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        val featureDomain = path.substringBeforeLast(':') + ":domain"
        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                api(project(featureDomain))
                implementation(project(":core:common"))
                implementation(project(":core:network"))
                implementation(libs.lib("kotlinx-serialization-json"))
                implementation(libs.lib("kotlinx-datetime"))
                implementation(libs.lib("ktor-client-core"))
            }
        }
    }
}

class FeaturePresentationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("pharmatrade.kmp.compose")

        val featureDomain = path.substringBeforeLast(':') + ":domain"
        kotlinMultiplatform {
            sourceSets.getByName("commonMain").dependencies {
                // Some features (e.g. search) are presentation-only and have no :domain of their own.
                if (findProject(featureDomain) != null) api(project(featureDomain))
                implementation(project(":core:common"))
                implementation(project(":core:ui"))
                implementation(libs.lib("kotlinx-datetime"))
                implementation(libs.lib("coil-compose"))
                // api: public ViewModels extend androidx.lifecycle.ViewModel, so consumers need it too.
                api(libs.lib("androidx-lifecycle-viewmodel"))
                implementation(libs.lib("androidx-lifecycle-viewmodel-compose"))
                implementation(libs.lib("androidx-lifecycle-runtime-compose"))
            }
        }
    }
}
