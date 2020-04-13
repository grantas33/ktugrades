import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder.createConnectionPool

group = "org.ktugrades"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.70" apply false
    kotlin("multiplatform") version "1.3.70" apply false
    kotlin("js") version "1.3.70" apply false
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

    val configFile = file("${project("backend").projectDir}/src/main/resources/application.conf")
    val configObj = groovy.util.ConfigSlurper().parse(configFile.toURI().toURL())
    val databaseConfig = configObj["database"] as groovy.util.ConfigObject

    val connection = createConnectionPool(
        databaseConfig["url"].toString() +
            "?user=${databaseConfig["user"].toString()}" +
            "&password=${databaseConfig["password"].toString()}"
    )

    val sql = """
        drop table if exists Mark;
        drop table if exists MarkInformation;
        drop table if exists Module;
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

        create table Module
        (
        	code varchar(255) not null,
        	semesterNumber varchar(255) not null,
        	title varchar(255) null,
        	professor varchar(255) null,
        	constraint Module_pk
        		primary key (code, semesterNumber)
        );
        
        create table MarkInformation
        (
        	id int auto_increment,
        	moduleCode varchar(255) not null,
        	semesterNumber varchar(255) not null,
            userId binary(16) not null,
        	typeId varchar(255) null,
        	week varchar(255) not null,
        	constraint MarkInformation_pk
        		primary key (id),
        	constraint MarkInformation_Module_code_semesterNumber_fk
        		foreign key (moduleCode, semesterNumber) references Module (code, semesterNumber)
            constraint MarkInformation_User_username_fk
                foreign key (userId) references User (username) on delete cascade,
        );
        
        create table Mark
        (
        	id int auto_increment,
        	markInformationId int not null,
        	mark varchar(255) not null,
        	constraint Mark_pk
        		primary key (id),
        	constraint Mark_MarkInformation_id_fk
        		foreign key (markInformationId) references MarkInformation (id) on delete cascade
        );

    """.trimIndent()

    sql.split(";").forEach {
            connection.sendQuery(it)
    }
}