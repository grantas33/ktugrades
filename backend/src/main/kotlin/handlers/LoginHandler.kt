package org.ktugrades.backend.handlers

import org.ktugrades.backend.*
import io.ktor.client.features.cookies.cookies
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.*
import org.jsoup.Jsoup

// TODO: handle case when password change is required
class LoginHandler {

    suspend fun getAuthCookie(username: String, password: String): String {
        return try {
            val postLogin = postLogin(username, password, getAuthState())
            getStudCookie(postLogin.samlResponse, postLogin.relayState)
        } catch (e: Exception) {
            null
        } ?: throw IllegalArgumentException("Unable to get the cookie for the student.")
    }

    data class PostLoginResponse(val samlResponse: String, val relayState: String)

    private suspend fun getAuthState(): String =
        getClient().get<HttpResponse> {
            url("https://uais.cr.ktu.lt/ktuis/studautologin")
        }.let {
            Jsoup.parse(it.readText())
                .select("input[name=\"AuthState\"]")
                .first()
                .attr("value")
        }

    private suspend fun postLogin(username: String, password: String, authState: String): PostLoginResponse =
        getClient().post<HttpResponse> {
            url("https://login.ktu.lt/simplesaml/module.php/KTU/loginuserpass.php")
            body = FormDataContent(Parameters.build {
                append("username", username)
                append("password", password)
                append("AuthState", authState)
            })
        }.let {
            val redirect = it.headers["Location"].toString()
            getClient().get<HttpResponse> {
                url(redirect)
            }
        }.let {
            Jsoup.parse(it.readText())
                .select("input")
        }.let {
            PostLoginResponse(
                samlResponse = it.first { it.attr("name") == "SAMLResponse" }.attr("value"),
                relayState = it.first { it.attr("name") == "RelayState" }.attr("value")
            )
        }

    private suspend fun getStudCookie(samlResponse: String, relayState: String): String? {
         getClient().apply {
            post<HttpResponse> {
                url("https://uais.cr.ktu.lt/shibboleth/SAML2/POST")
                body = FormDataContent(Parameters.build {
                    append("SAMLResponse", samlResponse)
                    append("RelayState", relayState)
                })
            }

            get<HttpResponse> {
                url("https://uais.cr.ktu.lt/ktuis/studautologin")
            }

            cookies("https://uais.cr.ktu.lt/ktuis/studautologin")
                .find { it.name == "STUDCOOKIE" }
                ?.let {
                    return it.value
                }
        }
        return null
    }
}