pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Pharma Trade"
include(":app")
include(":shared")
include(":desktopApp")

// Core
include(":core:common")
include(":core:network")
include(":core:ui")

// Features: each split into domain / data / presentation
include(":feature:admin:domain", ":feature:admin:data", ":feature:admin:presentation")
include(":feature:auth:domain", ":feature:auth:data", ":feature:auth:presentation")
include(":feature:cart:domain", ":feature:cart:data", ":feature:cart:presentation")
include(":feature:catalog:domain", ":feature:catalog:data", ":feature:catalog:presentation")
include(":feature:drugs:domain", ":feature:drugs:data")
include(":feature:notification:domain", ":feature:notification:data", ":feature:notification:presentation")
include(":feature:pharmacyorder:domain", ":feature:pharmacyorder:data", ":feature:pharmacyorder:presentation")
include(":feature:profile:domain", ":feature:profile:data", ":feature:profile:presentation")
include(":feature:search:presentation")
include(":feature:seller:domain", ":feature:seller:data", ":feature:seller:presentation")
include(":feature:supplierorder:domain", ":feature:supplierorder:data", ":feature:supplierorder:presentation")