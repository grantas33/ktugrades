package org.ktugrades.backend

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.json
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.ktugrades.common.*
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.security.Security

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    Security.addProvider(BouncyCastleProvider())
    val dependencies = Dependencies(environment, jsonSerializer)
    val logger = LoggerFactory.getLogger("app")

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
        post(Routes.Authenticate) {
            val credentials = call.receive<Credentials>().let {
                it.copy(username = it.username.toLowerCase())
            }

            withCoroutineClient {
                dependencies.loginHandler.getAuthCookie(credentials.username, credentials.password)
            }
            val encryptedUsername = dependencies.encryptionService.encrypt(credentials.username)
            val encryptedPassword = dependencies.encryptionService.encrypt(credentials.password)
            dependencies.mySqlProvider.upsertUser(username = encryptedUsername, password = encryptedPassword)
            call.respond(HttpStatusCode.OK, EncryptedUsername(username = encryptedUsername))
        }
        get(Routes.Grades) {
            val queryParameters: Parameters = call.request.queryParameters
            val encrypted = jsonSerializer.parse(EncryptedUsername.serializer(), queryParameters["username"]!!)
            val credentials = dependencies.credentialProvider.getCredentials(encrypted.username)
            withCoroutineClient {
                dependencies.run {
                    loginHandler.getAuthCookie(credentials.username, credentials.password)
                    markService.getMarks(encrypted.username).let {
                        mySqlProvider.insertNewMarkInformation(marks = it.markInfoToAddAndNotify, user = encrypted.username)
                        mySqlProvider.updateMarks(it.markInfoToUpdate + it.markInfoToUpdateAndNotify, encrypted.username)
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = it.latestMarks
                        )
                    }
                }
            }
        }
        post (Routes.Subscription) {
            val payload = call.receive<SubscriptionPayload>()
            dependencies.mySqlProvider.insertUserSubscription(payload)
            call.respond(HttpStatusCode.OK)
        }
        post (Routes.Unsubscription) {
            val payload = call.receive<UnsubscriptionPayload>()
            dependencies.mySqlProvider.deleteUserSubscription(payload)
            call.respond(HttpStatusCode.OK)
        }
    }

    dependencies.schedulerService.runEveryInterval(minutes = 30) {
        launch {
            dependencies.mySqlProvider.apply {
                val markSlotAverages = getAverageMarks()
                insertMarkSlotAverages(markSlotAverages)
            }
            dependencies.notificationService.broadcastToAll(
                onSingleUserError =  { logger.warn(it.localizedMessage) }
            )
        }
    }
}


