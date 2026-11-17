plugins {
    id("alwayscast.android.library")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.matiribco.alwayscast.core.cast"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.play.services.cast.framework)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
}
