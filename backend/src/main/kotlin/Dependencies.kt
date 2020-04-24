package org.ktugrades.backend

import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import org.ktugrades.backend.handlers.DataHandler
import org.ktugrades.backend.handlers.LoginHandler
import io.ktor.application.ApplicationEnvironment
import org.ktugrades.backend.services.*

@Suppress("unused", "EXPERIMENTAL_API_USAGE") // Referenced in application.conf
class Dependencies(private val appEnvironment: ApplicationEnvironment) {

    val loginHandler by lazy { LoginHandler() }
    private val dataHandler by lazy { DataHandler() }
    val markService by lazy {
        MarkService(dataHandler = dataHandler, mySqlProvider = mySqlProvider)
    }

    val notificationService by lazy {
        NotificationService(mySqlProvider = mySqlProvider)
    }

    val encryptionService by lazy {
        EncryptionService(
            key = appEnvironment.config.config("encryption").property("key").getString()
        )
    }

    val credentialProvider by lazy {
        CredentialProvider(
            userProvider = { mySqlProvider.getUser(it) },
            encryptionService = encryptionService
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

