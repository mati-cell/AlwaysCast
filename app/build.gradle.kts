plugins {
    id("alwayscast.android.application")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.matiribco.alwayscast"

    defaultConfig {
        applicationId = "dev.matiribco.alwayscast"
        versionCode = 1
        versionName = libs.versions.app.get()
    }
}

dependencies {
    implementation(project(":core:common"))

    testImplementation(libs.junit)
}
