import org.ktugrades.common.SubscriptionPayload
import kotlinx.coroutines.*
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.Uint8Array
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import react.*
import react.dom.h1
import styled.styledButton
import kotlin.browser.window

val toUint8Array = kotlinext.js.require("urlb64touint8array") as (base64String: String) -> IntArray

interface MainPageProps: RProps {
    var pushManager: PushManager
}
interface MainPageState: RState {
    var isSubscribedToPush: Boolean
}

class MainPage: RComponent<MainPageProps, MainPageState>() {

    override fun MainPageState.init() {
        isSubscribedToPush = false

        MainScope().launch {
            props.pushManager.getSubscription().await().let {
                setState {
                    isSubscribedToPush = it != null
                }
            }
        }
    }

    private fun subscribeUser() = GlobalScope.launch {
        val encoded = toUint8Array(PUSH_API_PUBLIC_KEY)
        try {
            val subscription = props.pushManager.subscribe(
                PushSubscriptionOptions(userVisibleOnly = true, applicationServerKey = encoded)
            ).await()
            sendSubscriptionToServer(subscription)
            console.log("Subscribed.")
            setState {
                isSubscribedToPush = true
            }
        } catch (e: Exception) {
            console.warn("Subscription denied.")
        }
    }

    private suspend fun sendSubscriptionToServer(subscription: PushSubscription): Response {
        val payload = SubscriptionPayload(
            endpoint = subscription.endpoint,
            key = subscription.getKey("p256dh")
                .let { window.btoa(js("String").fromCharCode.apply(null, Uint8Array(it)) as String) },
            auth = subscription.getKey("auth")
                .let { window.btoa(js("String").fromCharCode.apply(null, Uint8Array(it)) as String) }
        )

        return window.fetch("${SERVER_URL}/subscription", RequestInit(
            method = "POST",
            headers = applicationJsonHeaders,
            body = JSON.stringify(payload)
        )).await()
    }

    override fun RBuilder.render() {
        flexBox {
            if (state.isSubscribedToPush) {
                h1 {
                    +"Grades"
                }
            } else {
                styledButton {
                    attrs {
                        onClickFunction = { subscribeUser() }
                    }
                    +"Subscribe to push notifications to use this app."
                }
            }
        }
    }
}

fun RBuilder.mainPage(handler: MainPageProps.() -> Unit): ReactElement {
    return child(MainPage::class) {
        this.attrs(handler)
    }
}
