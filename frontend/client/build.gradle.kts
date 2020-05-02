plugins {
    kotlin("plugin.serialization")
}

repositories {
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains:kotlin-styled:1.0.0-pre.93-kotlin-1.3.70")
    implementation(npm("styled-components"))
    implementation(npm("inline-style-prefixer"))

    implementation("org.jetbrains:kotlin-react:16.13.0-pre.93-kotlin-1.3.70")
    implementation("org.jetbrains:kotlin-react-dom:16.13.0-pre.93-kotlin-1.3.70")
    implementation(npm("text-encoding"))
    implementation(npm("abort-controller"))
    implementation("io.ktor:ktor-client-js:1.3.2")
    implementation("io.ktor:ktor-client-json-js:1.3.2")
    implementation("io.ktor:ktor-client-serialization-js:1.3.2")
    implementation(npm("fs"))

    implementation(npm("react", "16.13.1"))
    implementation(npm("react-dom", "16.13.1"))
    implementation(npm("react-loader-spinner", "3.1.14"))
    implementation(project(":frontend:commonFrontend"))
}

kotlin.target.browser {
    @Suppress("EXPERIMENTAL_API_USAGE")
    dceTask {
        keep("ktor-ktor-io.\$\$importsForInline\$\$.ktor-ktor-io.io.ktor.utils.io")
    }
}