plugins {
    alias(libs.plugins.pharmatrade.feature.presentation)
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}
