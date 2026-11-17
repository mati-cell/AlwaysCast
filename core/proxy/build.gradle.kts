plugins {
    id("alwayscast.jvm.library")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(project(":core:model"))

    testImplementation(libs.junit)
}
