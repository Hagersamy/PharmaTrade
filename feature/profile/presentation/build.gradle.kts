plugins {
    alias(libs.plugins.pharmatrade.feature.presentation)
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":feature:auth:domain"))
        implementation(project(":feature:pharmacyorder:domain"))
    }
}
