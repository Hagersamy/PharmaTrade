plugins {
    `kotlin-dsl`
}

group = "com.pharmatrade.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // compileOnly: the real plugin versions come from the root build's `plugins { ... apply false }`.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "pharmatrade.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "pharmatrade.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("featureDomain") {
            id = "pharmatrade.feature.domain"
            implementationClass = "FeatureDomainConventionPlugin"
        }
        register("featureData") {
            id = "pharmatrade.feature.data"
            implementationClass = "FeatureDataConventionPlugin"
        }
        register("featurePresentation") {
            id = "pharmatrade.feature.presentation"
            implementationClass = "FeaturePresentationConventionPlugin"
        }
    }
}
