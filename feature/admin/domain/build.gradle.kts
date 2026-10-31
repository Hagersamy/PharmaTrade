plugins {
    alias(libs.plugins.pharmatrade.feature.domain)
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.coroutines.test)
    }
}
