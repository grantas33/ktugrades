object Versions {
    const val kotlin = "1.3.70"
    const val ktor = "1.3.2"
    const val logback = "1.2.3"
    const val jsoup = "1.11.3"
    const val webPush = "5.1.0"
    const val bouncyCastle = "1.64"
    const val jasync = "1.0.14"
    const val serialization = "0.20.0"
    const val mockk = "1.9.3"
    const val junit = "5.6.1"
    const val coroutines = "1.3.5"
}

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
    maven(url = "https://kotlin.bintray.com/ktor")
    maven(url = "https://kotlin.bintray.com/kotlinx")
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("io.ktor:ktor-server-core:${Versions.ktor}")
    implementation("io.ktor:ktor-client-core:${Versions.ktor}")
    implementation("io.ktor:ktor-client-core-jvm:${Versions.ktor}")
    implementation("io.ktor:ktor-client-json-jvm:${Versions.ktor}")
    implementation("io.ktor:ktor-client-gson:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization:${Versions.ktor}")
    implementation("org.jsoup:jsoup:${Versions.jsoup}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    implementation("nl.martijndwars:web-push:${Versions.webPush}")
    implementation("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastle}")
    implementation("org.bouncycastle:bcpkix-jdk15on:${Versions.bouncyCastle}")
    implementation("com.github.jasync-sql:jasync-mysql:${Versions.jasync}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Versions.serialization}")
    implementation(project(":common"))

    testImplementation("org.junit.jupiter:junit-jupiter-engine:${Versions.junit}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Versions.junit}")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("io.ktor:ktor-client-mock-jvm:${Versions.ktor}")
}
