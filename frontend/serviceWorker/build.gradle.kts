import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput.Target

plugins {
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":frontend:commonFrontend"))
}

tasks.register<Copy>("copyDistribution") {
    group = "build"
    description = "Copies a distribution to the root project distribution."

    from("$buildDir/distributions")
    into("${parent?.buildDir}/distributions")
}

tasks["build"].finalizedBy("copyDistribution")

kotlin {
    target {
        browser {
            webpackTask {
                output.libraryTarget = Target.SELF
            }
        }
    }
}
