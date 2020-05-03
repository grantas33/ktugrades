import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput.Target

plugins {
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":frontend:commonFrontend"))
}

kotlin {
    target {
        browser {
            webpackTask {
                output.libraryTarget = Target.SELF
            }
        }
    }
}
