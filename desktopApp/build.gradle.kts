plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass.set("com.pharmatrade.desktop.MainKt")
}
