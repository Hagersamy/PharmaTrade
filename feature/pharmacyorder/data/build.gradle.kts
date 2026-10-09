plugins {
    alias(libs.plugins.pharmatrade.feature.data)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(libs.multiplatform.settings)
    }
}
