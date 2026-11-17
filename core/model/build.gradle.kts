plugins {
    id("alwayscast.jvm.library")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

dependencies {
    testImplementation(libs.junit)
}
