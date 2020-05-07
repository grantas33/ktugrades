package org.ktugrades.backend

import com.github.jasync.sql.db.util.length
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.cookies.AcceptAllCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.cookies.cookies
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.http.hostWithPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CoroutineClientTest {
    @ExperimentalCoroutinesApi
    @Test
    fun `It should use a new client when running from a new coroutine context`() = runBlockingTest {

        suspend fun saveCookie() {
            getClient().get<HttpResponse> {
                url("https://uais.cr.ktu.lt/ktuis/studautologin")
            }
        }

        withTestCoroutineClient {
            saveCookie()
            getClient().cookies("https://uais.cr.ktu.lt/ktuis/studautologin")
                .find { it.name == "STUDCOOKIE" }
                .let {
                    assertEquals(expected = "mock-cookie", actual = it?.value)
                }
        }

        withTestCoroutineClient {
            assertEquals(
                expected = 0,
                actual = getClient().cookies("https://uais.cr.ktu.lt/ktuis/studautologin").length
            )
        }
    }
}

private suspend fun <T> withTestCoroutineClient(block: suspend CoroutineScope.() -> T): T {
    @Suppress("EXPERIMENTAL_API_USAGE")
    val client = CoroutineClient(
        client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullUrl) {
                        "https://uais.cr.ktu.lt/ktuis/studautologin" -> {
                            val responseHeaders = headersOf(
                                "Set-Cookie" to listOf("STUDCOOKIE=mock-cookie")
                            )
                            respond("mock-text", headers = responseHeaders)
                        }
                        else -> error("Unhandled ${request.url.fullUrl}")
                    }
                }
            }
            install(HttpCookies) { storage = AcceptAllCookiesStorage() }
        }
    )

    return withContext(client) {
        block()
    }
}

private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

