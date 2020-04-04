import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder.createConnectionPool

object Versions {
    const val kotlin = "1.3.70"
    const val ktor = "1.3.0"
    const val logback = "1.2.3"
    const val jsoup = "1.11.3"
    const val webPush = "5.1.0"
    const val bouncyCastle = "1.64"
    const val jasync = "1.0.14"
}

plugins {
    kotlin("jvm")
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
    implementation("io.ktor:ktor-gson:${Versions.ktor}")
    implementation("org.jsoup:jsoup:${Versions.jsoup}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    implementation("nl.martijndwars:web-push:${Versions.webPush}")
    implementation("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastle}")
    implementation("com.github.jasync-sql:jasync-mysql:${Versions.jasync}")
    testImplementation("io.ktor:ktor-server-tests:${Versions.ktor}")
    implementation(project(":common"))
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.github.jasync-sql:jasync-mysql:1.0.14")
    }
}

tasks.register("refreshDatabaseTables") {
    description = "Deletes any existing tables and creates new ones."

    val configFile = file("${projectDir}/src/main/resources/application.conf")
    val configObj = groovy.util.ConfigSlurper().parse(configFile.toURI().toURL())
    val databaseConfig = configObj["database"] as groovy.util.ConfigObject

    val connection = createConnectionPool(
            databaseConfig["url"].toString() +
                "?user=${databaseConfig["user"].toString()}" +
                "&password=${databaseConfig["password"].toString()}"
        )

    val sql = """
        drop table if exists UserSubscriptions;
        drop table if exists User;
        
        create table User
        (
            username binary(16) not null,
            password binary(16) not null,
            constraint User_pk primary key (username)
        );
        
        create table UserSubscriptions
        (
            id int auto_increment,
            userId binary(16) not null,
            endpoint varchar(255) not null,
            publicKey varchar(255) not null,
            auth varchar(255) not null,
            constraint UserSubscriptions_pk primary key (id),
            constraint UserSubscriptions_User_username_fk foreign key (userId) references User (username) on delete cascade,
            unique key UserSubscriptions_endpoint_publicKey_auth_uindex (endpoint, publicKey, auth)
        );
    """.trimIndent()

    sql.split(";").forEach {
        connection.sendQuery(it)
    }
}


