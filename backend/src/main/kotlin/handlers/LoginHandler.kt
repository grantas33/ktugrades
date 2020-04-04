package org.ktugrades.backend.handlers

import org.ktugrades.backend.*
import org.ktugrades.backend.models.LoginModel
import org.ktugrades.backend.models.YearModel
import io.ktor.client.features.cookies.cookies
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.lang.RuntimeException

class LoginHandler {

    suspend fun getAuthCookie(username: String, password: String): String? {
        return try {
            val postLogin = postLogin(username, password, getAuthState())
            getStudCookie(postLogin.samlResponse, postLogin.relayState)
        } catch (e: Exception) {
            null
        }
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

    private suspend fun getInfo(cookie: String): LoginModel {
        val call = getClient().get<HttpResponse> {
            url("https://uais.cr.ktu.lt/ktuis/vs.ind_planas")
        }

        val parse =  Jsoup.parse(call.readText())
        val nameItemText = parse.select("#ais_lang_link_lt").parents().first().text()
        val studentId = nameItemText.split(' ')[0].trim()
        val studentName = nameItemText.split(' ')[1].trim()
        val studyYears = parse.select(".ind-lst.unstyled > li > a")
        val yearRegex = "plano_metai=([0-9]+)".toRegex()
        val idRegex = "p_stud_id=([0-9]+)".toRegex()
        val studyList = studyYears.map { yearHtml ->
            yearHtml.attr("href").let { link ->
                YearModel(
                    id = idRegex.find(link)!!.groups[1]!!.value,
                    year = yearRegex.find(link)!!.groups[1]!!.value
                )
            }
        }.toMutableList()

        val calCall = getClient().get<HttpResponse> {
            url("https://uais.cr.ktu.lt/ktuis/TV_STUD.stud_kal_w0")
        }

        val calParse = Jsoup.parse(calCall.readText())
        val currentWeekElement = calParse.select("#kal_div_id").select("option[selected]")[1]
        val weekRegex = "(?:selected\\>)([0-9]*)".toRegex()
        val currentWeek = weekRegex.find(currentWeekElement.toString())!!.groupValues[1]
        return LoginModel(
                studCookie = cookie,
                studentName =studentName,
                studentId = studentId,
                currentWeek = currentWeek,
                studentSemesters = studyList
        )
    }
}