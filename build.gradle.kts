import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder.createConnectionPool

group = "org.ktugrades"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.70" apply false
    kotlin("multiplatform") version "1.3.70" apply false
    kotlin("js") version "1.3.70" apply false
    kotlin("plugin.serialization") version "1.3.70" apply false
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.github.jasync-sql:jasync-mysql:1.0.14")
    }
}

enum class Environment(val value: String) { MAIN("main"), TEST("test")}

fun refreshDatabaseTables(environment: Environment) {
    val configFile = file("${project("backend").projectDir}/src/${environment.value}/resources/application.conf")
    val configObj = groovy.util.ConfigSlurper().parse(configFile.toURI().toURL())
    val databaseConfig = configObj["database"] as groovy.util.ConfigObject

    val connection = createConnectionPool(
        databaseConfig["url"].toString() +
            "?user=${databaseConfig["user"].toString()}" +
            "&password=${databaseConfig["password"].toString()}"
    )

    val database = File("backend/mysql/database.sql").readText()
    database.split(";").forEach {
        connection.sendQuery(it)
    }

    if (environment == Environment.TEST) {
        val fixtures = File("backend/mysql/fixtures.sql").readText()
        fixtures.split(";").forEach {
            connection.sendQuery(it)
        }
    }
}

tasks.register("refreshProdDatabaseTables") {
    description = "Deletes any existing tables and creates new ones."
    refreshDatabaseTables(Environment.MAIN)
}

tasks.register("refreshTestDatabaseTables") {
    description = "Deletes any existing tables and creates new ones."
    refreshDatabaseTables(Environment.TEST)
}