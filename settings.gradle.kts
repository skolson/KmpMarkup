pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

val projectNameMavenName = "kmp-markup"
rootProject.name = projectNameMavenName

include(":KmpMarkup")
project( ":KmpMarkup" ).name = projectNameMavenName
