plugins {
    `kotlin-dsl`
}

group = "dev.matiribco.alwayscast.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "alwayscast.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "alwayscast.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "alwayscast.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
