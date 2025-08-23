import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform

plugins {
    libs.plugins.also {
        alias(it.kotlin.multiplatform)
        alias(it.android.library)
        alias(it.dokka.base)
        alias(it.maven.publish.vannik)
    }
}
val appVersion = libs.versions.appVersion.get()
group = libs.versions.appId.get()
version = libs.versions.appVersionName.get()
val appleFrameworkName = "KmpMarkup"
val publishDomain = "io.github.skolson"
val githubUri = "skolson/$appleFrameworkName"
val githubUrl = "https://github.com/$githubUri"

group = publishDomain
version = appVersion

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
                baseName = appleFrameworkName
            }
        }
    }
    iosX64 {
        binaries {
            framework {
                baseName = appleFrameworkName
            }
        }
    }
    iosSimulatorArm64 {
        binaries {
            framework {
                baseName = appleFrameworkName
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
    linuxArm64() {
        binaries {
            executable {
                debuggable = true
            }
        }
    }
    jvm()
    macosArm64 {
        binaries {
            framework {
                baseName = appleFrameworkName
            }
        }
    }
    macosX64 {
        binaries {
            framework {
                baseName = appleFrameworkName
            }
        }
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kmp.io)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.bigdecimal)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.kotlin.test)
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

mavenPublishing {
    coordinates(publishDomain, name, appVersion)
    configure(
        KotlinMultiplatform(
            JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            true,
            listOf("debug", "release")
        )
    )

    pom {
        name.set("Kotlin Multiplatform XML Parser")
        description.set("Library for simple XML parsing on supported 64 bit platforms; Android, IOS, Windows, Linux, MacOS")
        url.set(githubUrl)
        inceptionYear.set("2025")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("oldguy")
                name.set("Steve Olson")
                email.set("skolson5903@gmail.com")
                url.set("https://github.com/skolson/")
            }
        }
        scm {
            url.set(githubUrl)
            connection.set("scm:git:git://git@github.com:${githubUri}.git")
            developerConnection.set("scm:git:ssh://git@github.com:${githubUri}.git")
        }
    }
}
