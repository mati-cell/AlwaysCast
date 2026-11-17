plugins {
    id("alwayscast.jvm.library")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.core)
}
