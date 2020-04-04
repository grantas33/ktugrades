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
    }

    tasks.register<Copy>("copyDistribution") {
        group = "build"
        description = "Copies a distribution to the root project distribution."

        from("$buildDir/distributions")
        into("${parent?.buildDir}/distributions")
    }

    tasks["build"].finalizedBy("copyDistribution")
}
