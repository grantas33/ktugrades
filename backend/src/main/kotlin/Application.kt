package org.ktugrades.backend

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.ktugrades.common.*
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.security.Security

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    Security.addProvider(BouncyCastleProvider())
    val dependencies = Dependencies(environment)
    val logger = LoggerFactory.getLogger("app")
    val jsonSerializer = Json(configuration = JsonConfiguration.Stable)

    install(CORS) {
        method(HttpMethod.Options)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(ContentNegotiation) {
        json(json = jsonSerializer)
    }

    install(StatusPages) {
        exception<IllegalArgumentException> {
            logger.warn(it.localizedMessage, it)
            call.respond(HttpStatusCode.BadRequest, ErrorMessage(it.localizedMessage))
        }
        exception<Exception> {
            logger.error(it.localizedMessage, it)
            call.respond(HttpStatusCode.InternalServerError, ErrorMessage(it.localizedMessage))
        }
    }

    routing {
        post("/authenticate") {
            val credentials = call.receive<Credentials>()
            withCoroutineClient {
                dependencies.loginHandler.getAuthCookie(credentials.username, credentials.password)
            }
            val encryptedUsername = dependencies.encryptionService.encrypt(credentials.username)
            val encryptedPassword = dependencies.encryptionService.encrypt(credentials.password)
            dependencies.mySqlProvider.upsertUser(username = encryptedUsername, password = encryptedPassword)
            call.respond(HttpStatusCode.OK, EncryptedUsername(username = encryptedUsername))
        }
        post("/grades") {
            val encrypted = call.receive<EncryptedUsername>()
            val credentials = dependencies.credentialProvider.getCredentials(encrypted.username)
            withCoroutineClient {
                dependencies.run {
                    loginHandler.getAuthCookie(credentials.username, credentials.password)
                    markService.getMarks(encrypted.username).let {
                        mySqlProvider.insertModules(it.newModules)
                        mySqlProvider.insertMarks(marks = it.markInfoToAddAndNotify, user = encrypted.username)
                        mySqlProvider.updateMarks(it.markInfoToUpdate + it.markInfoToUpdateAndNotify, encrypted.username)
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = it.latestMarks.map { it.toResponse() }
                        )
                    }
                }
            }
        }
        post ("/subscription") {
            val payload = call.receive<SubscriptionPayload>()
            dependencies.mySqlProvider.insertUserSubscription(payload)
            call.respond(HttpStatusCode.OK)
        }
        post("/broadcast") {
            dependencies.notificationService.broadcastToAll()
        }
    }
}
