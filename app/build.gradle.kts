plugins {
    id("alwayscast.android.application")
    alias(libs.plugins.kotlin.compose)
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":feature:cast"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:proxy"))
    implementation(project(":core:cast"))
    implementation(project(":core:common"))

    implementation(libs.okhttp)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
}
