package org.ktugrades.backend

import org.ktugrades.common.AuthenticationResponse
import org.ktugrades.common.Credentials
import org.ktugrades.common.ErrorMessage
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.cookies.AcceptAllCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.ktugrades.common.SubscriptionPayload
import java.security.Security

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    Security.addProvider(BouncyCastleProvider())
    val dependencies = Dependencies(environment)

    install(CORS) {
        method(HttpMethod.Options)
        allowCredentials = true
        allowNonSimpleContentTypes = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            serializeNulls()
        }
    }

    routing {
        post("/authenticate") {
            @Suppress("EXPERIMENTAL_API_USAGE")
            val client = CoroutineClient(
                client = HttpClient(CIO) {
                    install(JsonFeature) { serializer = GsonSerializer() }
                    install(HttpCookies) { storage = AcceptAllCookiesStorage() }
                }
            )
            val credentials = call.receive<Credentials>()
            val cookie = withContext(client) {
                dependencies.loginHandler.getAuthCookie(credentials.username, credentials.password)
            }
            cookie?.let {
                val encryptedUsername = dependencies.encryptionService.encrypt(credentials.username)
                val encryptedPassword = dependencies.encryptionService.encrypt(credentials.password)
                dependencies.mySqlProvider.upsertUser(username = encryptedUsername, password = encryptedPassword)
                call.respond(HttpStatusCode.OK, AuthenticationResponse(username = encryptedUsername, password = encryptedPassword))
            } ?:
            call.respond(HttpStatusCode.BadRequest, ErrorMessage("Authentication failed."))
        }
        post ("/subscription") {
            val payload = call.receive<SubscriptionPayload>()
            dependencies.mySqlProvider.insertUserSubscription(payload)
            call.respond(HttpStatusCode.OK)
        }
    }
}
