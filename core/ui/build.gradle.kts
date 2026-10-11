// Design system: theme (colors, typography) and composables reused across features.
plugins {
    alias(libs.plugins.pharmatrade.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(libs.coil.compose)
            // api: screens hosting the glass bar mark their content as the blur source (Modifier.haze).
            api(libs.haze)
        }
        androidMain.dependencies {
            // Theme.android.kt (WindowCompat / status bar appearance)
            implementation(libs.androidx.core.ktx)
        }
    }
}
