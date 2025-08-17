plugins {
    libs.plugins.also {
        alias(it.kotlin.multiplatform)
        alias(it.android.library)
    }
}
val appVersion = libs.versions.appVersion.get()
group = libs.versions.appId.get()
version = libs.versions.appVersionName.get()

group = "com.oldguy"
version = appVersion
val moduleName = "KmpMarkup"

val javaLanguageVersion = JavaLanguageVersion.of(libs.versions.javaVersion.get().toInt())
val javaVersion = JavaVersion.toVersion(libs.versions.javaVersion.get())

java {
    toolchain {
        languageVersion.set(javaLanguageVersion)
    }
}

android {
    compileSdk = libs.versions.androidSdk.get().toInt()
    buildToolsVersion = libs.versions.androidBuildTools.get()
    namespace = "${libs.versions.appId.get()}.common"

    defaultConfig {
        minSdk = libs.versions.androidSdkMinimum.get().toInt()
        testInstrumentationRunner = libs.versions.androidxTestRunner.get()
        buildFeatures {
            buildConfig = false
        }
        testInstrumentationRunnerArguments["runnerBuilder"] = libs.versions.testRunnerBuilder.get()
    }
}

kotlin {
    jvmToolchain {
        languageVersion = javaLanguageVersion
    }
    androidTarget {
        java.sourceCompatibility = javaVersion
        java.targetCompatibility = javaVersion
    }

    iosArm64 {
        binaries {
            framework {
                baseName = moduleName
            }
        }
    }
    iosX64 {
        binaries {
            framework {
                baseName = moduleName
            }
        }
    }
    iosSimulatorArm64 {
        binaries {
            framework {
                baseName = moduleName
            }
        }
    }
    linuxX64() {
        binaries {
            executable {
                debuggable = true
            }
        }
    }
    jvm()
    linuxArm64() {
        binaries {
            executable {
                debuggable = true
            }
        }
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kmp.io)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.kotlin.test)
                implementation(libs.kotlinx.datetime)
            }
        }
        val androidMain by getting {
        }
        val linuxMain by getting {
        }
        all {
            languageSettings {
                optIn("kotlin.ExperimentalUnsignedTypes")
            }
        }
    }
}