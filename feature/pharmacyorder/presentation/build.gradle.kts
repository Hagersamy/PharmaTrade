plugins {
    alias(libs.plugins.pharmatrade.feature.presentation)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":feature:drugs:domain"))
    }
}
