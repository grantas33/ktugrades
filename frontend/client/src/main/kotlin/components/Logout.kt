package components

import CACHE_USERNAME
import CacheProps
import DATA_CACHE
import PushManager
import PushSubscription
import SERVER_URL
import getAuthForRequest
import getKeyForRequest
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import jsonClient
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import org.ktugrades.common.Routes
import org.ktugrades.common.UnsubscriptionPayload
import react.*
import services.removeInCache
import styled.*
import kotlin.browser.window

interface LogoutProps: RProps, CacheProps {
    var pushManager: PushManager?
}

val Logout = functionalComponent<LogoutProps> { props ->

    suspend fun deleteSubscriptionRecord(subscription: PushSubscription) {
        val payload = UnsubscriptionPayload(
            endpoint = subscription.endpoint,
            key = subscription.getKeyForRequest(),
            auth = subscription.getAuthForRequest()
        )
        jsonClient.post<Unit>(SERVER_URL + Routes.Unsubscription) {
            contentType(ContentType.Application.Json)
            body = payload
        }
    }

    suspend fun deleteCachedUsername() = window.caches.removeInCache(cache = DATA_CACHE, key = CACHE_USERNAME)

    suspend fun unsubscribePushNotifications(subscription: PushSubscription) {
        try {
            subscription.unsubscribe().await()
            console.log("User unsubscribed")
        } catch (e: Exception) {
            console.error("User unsubscription failed: ${e.message}")
        }
    }

    fun unsubscribeUser() {
        MainScope().launch {
            val subscription = props.pushManager?.getSubscription()?.await()
            if (subscription != null) {
                deleteSubscriptionRecord(subscription)
                unsubscribePushNotifications(subscription)
            }
            deleteCachedUsername()
            props.setCredentialsExisting(false)
        }
    }

    appButton {
        css {
            position = Position.absolute
            top = LinearDimension("10px")
            right = LinearDimension("12%")
            mobileView {
                right = LinearDimension("10px")
            }
        }
        attrs {
            onClickFunction = {
                it.preventDefault()
                unsubscribeUser()
            }
        }
        +"Logout"
    }
}
