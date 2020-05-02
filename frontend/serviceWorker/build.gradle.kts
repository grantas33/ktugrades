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
            @Suppress("EXPERIMENTAL_API_USAGE")
            dceTask {
                keep("ktor-ktor-io.\$\$importsForInline\$\$.ktor-ktor-io.io.ktor.utils.io")
            }
            webpackTask {
                output.libraryTarget = Target.SELF
            }
        }
    }
}
