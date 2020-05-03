plugins {
    id("org.jetbrains.kotlin.js") apply false
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.js")
    }

    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        mavenCentral()
        jcenter()
    }

    val implementation by configurations

    dependencies {
        implementation(kotlin("stdlib-js"))
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.4")
        implementation(project(":common"))
    }
}
