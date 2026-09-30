// Umbrella module: re-exports every core + feature module to the platform apps (:app, :desktopApp,
// and the iOS Shared.framework) and hosts the Home shell that composes screens from several
// features — the one place allowed to depend on more than one feature's :presentation.
plugins {
    alias(libs.plugins.pharmatrade.kmp.compose)
}

val exportedModules = listOf(
    ":core:common",
    ":core:network",
    ":core:ui",
) + listOf(
    "admin", "auth", "cart", "catalog", "drugs", "notification",
    "pharmacyorder", "profile", "search", "seller", "supplierorder",
).flatMap { feature ->
    listOf("domain", "data", "presentation").map { ":feature:$feature:$it" }
}.filter { findProject(it) != null }

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            exportedModules.forEach { api(project(it)) }
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
    }
}
