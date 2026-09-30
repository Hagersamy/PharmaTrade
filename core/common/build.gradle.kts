// Shared building blocks used by every feature: Result, shared models, i18n strings, session,
// formatting utils, push-token / reminder plumbing and platform file IO.
plugins {
    alias(libs.plugins.pharmatrade.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            api(libs.multiplatform.settings)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            // FilePicker.android.kt (rememberLauncherForActivityResult / ActivityResultContracts)
            implementation(libs.androidx.activity.compose)
        }
    }
}
