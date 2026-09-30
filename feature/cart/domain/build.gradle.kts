plugins {
    alias(libs.plugins.pharmatrade.feature.domain)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}
