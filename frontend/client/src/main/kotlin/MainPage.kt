import org.ktugrades.common.SubscriptionPayload
import kotlinx.coroutines.*
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.Uint8Array
import org.w3c.dom.get
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import react.*
import react.dom.h1
import styled.styledButton
import kotlin.browser.localStorage
import kotlin.browser.window

val toUint8Array = kotlinext.js.require("urlb64touint8array") as (base64String: String) -> IntArray

interface MainPageProps: RProps, LocalStorageProps {
    var pushManager: PushManager
}
interface MainPageState: RState {
    var pushManagerState: PushManagerState
}

class MainPage: RComponent<MainPageProps, MainPageState>() {

    override fun MainPageState.init() {
        pushManagerState = PushManagerState.Loading

        MainScope().launch {
            props.pushManager.getSubscription().await().let {
                setState {
                    pushManagerState = if (it != null) PushManagerState.Subscribed else PushManagerState.NotSubscribed
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
                pushManagerState = PushManagerState.Subscribed
            }
        } catch (e: Exception) {
            console.warn("Subscription denied - ${e.message}")
        }
    }

    private suspend fun sendSubscriptionToServer(subscription: PushSubscription): Response {
        if (!isCredentialsExisting()) props.notifyLocalStorageUpdated()
        val payload = SubscriptionPayload(
                username = JSON.parse(localStorage["username"]!!),
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
            when (state.pushManagerState) {
                PushManagerState.Subscribed -> h1 {
                    +"Grades"
                }
                PushManagerState.NotSubscribed -> styledButton {
                    attrs {
                        onClickFunction = { subscribeUser() }
                    }
                    +"Subscribe to push notifications to receive a notification on this device upon receiving a mark."
                }
                PushManagerState.Loading -> loadingComponent()
            }
        }
    }
}

fun RBuilder.mainPage(handler: MainPageProps.() -> Unit): ReactElement {
    return child(MainPage::class) {
        this.attrs(handler)
    }
}
