import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<LibraryExtension> {
            compileSdk = AndroidDefaults.COMPILE_SDK
            buildToolsVersion = AndroidDefaults.BUILD_TOOLS

            defaultConfig {
                minSdk = AndroidDefaults.MIN_SDK
            }

            compileOptions {
                sourceCompatibility = AndroidDefaults.JAVA_VERSION
                targetCompatibility = AndroidDefaults.JAVA_VERSION
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                jvmTarget.set(AndroidDefaults.JVM_TARGET)
            }
        }
    }
}
