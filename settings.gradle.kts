pluginManagement {
    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }

        mavenCentral()

        maven { setUrl("https://plugins.gradle.org/m2/") }
    }
}

rootProject.name = "ktugrades"
include("backend")
include(":frontend:client")
include(":frontend:serviceWorker")
include("common")