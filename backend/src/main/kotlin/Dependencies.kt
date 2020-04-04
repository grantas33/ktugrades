package org.ktugrades.backend

import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import org.ktugrades.backend.handlers.DataHandler
import org.ktugrades.backend.handlers.LoginHandler
import org.ktugrades.backend.services.EncryptionService
import org.ktugrades.backend.services.MySqlProvider
import io.ktor.application.ApplicationEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.cookies.AcceptAllCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature

@Suppress("unused", "EXPERIMENTAL_API_USAGE") // Referenced in application.conf
class Dependencies(private val appEnvironment: ApplicationEnvironment) {

    val loginHandler by lazy { LoginHandler() }
    val dataHandler by lazy { DataHandler() }

    val encryptionService by lazy {
        EncryptionService(
            key = appEnvironment.config.config("encryption").property("key").getString()
        )
    }

    val dbConnection by lazy {
        appEnvironment.config.config("database").let {
            MySQLConnectionBuilder.createConnectionPool(
                it.property("url").getString() +
                    "?user=${it.property("user").getString()}" +
                    "&password=${it.property("password").getString()}"
            ).asSuspending
        }
    }

    val mySqlProvider by lazy {
        MySqlProvider(connection = dbConnection)
    }
}

