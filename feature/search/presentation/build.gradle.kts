plugins {
    alias(libs.plugins.pharmatrade.feature.presentation)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":feature:catalog:domain"))
        implementation(project(":feature:drugs:domain"))
        implementation(project(":feature:seller:domain"))
    }
}
