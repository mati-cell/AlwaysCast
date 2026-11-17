plugins {
    id("alwayscast.android.library")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.matiribco.alwayscast.feature.cast"
}

dependencies {
    implementation(project(":core:model"))

    testImplementation(libs.junit)
}
