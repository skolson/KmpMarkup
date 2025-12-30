import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    libs.plugins.also {
        alias(it.kotlin.multiplatform)
        alias(it.android.kmp.library)
        alias(it.dokka.base)
        alias(it.maven.publish.vannik)
    }
}
val appVersion = libs.versions.appVersion.get()
group = libs.versions.appId.get()
version = libs.versions.appVersionName.get()
val appleFrameworkName = "KmpMarkup"
val iosMinSdk = "14"
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

kotlin {
    jvmToolchain {
        languageVersion = javaLanguageVersion
    }

    androidLibrary {
        compileSdk = libs.versions.androidSdk.get().toInt()
        minSdk = libs.versions.androidSdkMinimum.get().toInt()
        buildToolsVersion = libs.versions.androidBuildTools.get()
        namespace = libs.versions.appId.get()

        withHostTest {}
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            execution = "HOST"
        }

        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.files.add(project.file("proguard-rules.pro"))
        }
    }

    val appleXcf = XCFramework()
    listOf(
        macosX64(), macosArm64(), iosX64(), iosArm64(), iosSimulatorArm64()
    ).forEach {
        it.binaries {
            framework {
                baseName = appleFrameworkName
                appleXcf.add(this)
                isStatic = true
                if (it.name.contains("ios")) {
                    freeCompilerArgs =
                        freeCompilerArgs + listOf("-Xoverride-konan-properties=osVersionMin=$iosMinSdk")
                }
            }
        }
    }
    linuxArm64()
    linuxX64()
    jvm()

    applyDefaultHierarchyTemplate()
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(libs.kmp.io)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.bigdecimal)
            }
        }
        getByName("commonTest") {
            dependencies {
                implementation(libs.bundles.kotlin.test)
            }
        }

        all {
            languageSettings {
                optIn("kotlin.ExperimentalUnsignedTypes")
            }
        }
    }
}

val longName = "Kotlin Multiplatform XML Parser"
dokka {
    moduleName.set(longName)
    dokkaSourceSets.commonMain {
    }
    dokkaPublications.html {
    }
}

mavenPublishing {
    coordinates(publishDomain, name, appVersion)
    configure(
        KotlinMultiplatform(
            JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            true
        )
    )

    pom {
        name.set(longName)
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
