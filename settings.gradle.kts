pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val projectNameMavenName = "kmp-markup"
rootProject.name = projectNameMavenName

include(":KmpMarkup")
project( ":KmpMarkup" ).name = projectNameMavenName
