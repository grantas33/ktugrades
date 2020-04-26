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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.20.0")

    implementation(npm("react", "16.13.1"))
    implementation(npm("react-dom", "16.13.1"))
    implementation(npm("react-loader-spinner", "3.1.14"))
    implementation(project(":common"))
}

kotlin.target.browser { }