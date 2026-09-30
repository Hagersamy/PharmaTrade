// Design system: theme (colors, typography) and composables reused across features.
plugins {
    alias(libs.plugins.pharmatrade.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(libs.coil.compose)
        }
        androidMain.dependencies {
            // Theme.android.kt (WindowCompat / status bar appearance)
            implementation(libs.androidx.core.ktx)
        }
    }
}
