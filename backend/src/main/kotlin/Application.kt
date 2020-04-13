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
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.ktugrades.common.SubscriptionPayload
import java.lang.RuntimeException
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
        post("/grades") {
            @Suppress("EXPERIMENTAL_API_USAGE")
            val client = CoroutineClient(
                client = HttpClient(CIO) {
                    install(JsonFeature) { serializer = GsonSerializer() }
                    install(HttpCookies) { storage = AcceptAllCookiesStorage() }
                }
            )
            val credentials = call.receive<Credentials>()
            val mod = withContext(client) {
                dependencies.run {
                    loginHandler.getAuthCookie(credentials.username, credentials.password) ?: throw RuntimeException("Unable to get the cookie for the student.")
                    loginHandler.setEnglishLanguageForClient()
                    loginHandler.getInfo().let {
                        it.studentSemesters.sortedByDescending { it.year }.take(2).map {
                            dataHandler.getGrades(planYear = it.year, studId = it.id)
                        }.flatten().sortedWith(compareBy ({ -it.semesterNumber.toInt() }, {-it.week.split("-").map {it.toInt()}.average()})).toList()
                    }
                }
            }
            call.respond(HttpStatusCode.BadRequest, ErrorMessage("Authentication failed."))

        }
        post ("/subscription") {
            val payload = call.receive<SubscriptionPayload>()
            dependencies.mySqlProvider.insertUserSubscription(payload)
            call.respond(HttpStatusCode.OK)
        }
        post("/broadcast") {
            val vapidKeyPair = getVapidKeyPair() ?: throw RuntimeException("Could not get VAPID key pair.")
            val pushService = PushService().apply {
                publicKey = vapidKeyPair.public
                privateKey = vapidKeyPair.private
            }
            val subscriptions = dependencies.mySqlProvider.getUserSubscriptions()
            subscriptions.forEach {
                val res = pushService.send(
                    Notification(
                        it.endpoint,
                        it.key,
                        it.auth,
                        "hello its the masrshian space jam is the artist"
                    )
                )
            }
        }
    }
}
