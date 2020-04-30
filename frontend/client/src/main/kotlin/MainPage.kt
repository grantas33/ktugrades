import components.flexBox
import components.grades
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.ktugrades.common.SubscriptionPayload
import kotlinx.coroutines.*
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.Uint8Array
import org.ktugrades.common.Routes
import react.*
import react.dom.div
import styled.styledButton
import kotlin.browser.window

interface MainPageProps: RProps, LocalStorageProps {
    var pushManager: PushManager
}
interface MainPageState: RState {
    var pushManagerState: PushManagerState
    var errorMessage: String
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
        try {
            val subscription = props.pushManager.subscribe(
                PushSubscriptionOptions(userVisibleOnly = true, applicationServerKey = PUSH_API_PUBLIC_KEY)
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

    private suspend fun sendSubscriptionToServer(subscription: PushSubscription) {
        if (!isCredentialsExisting()) props.notifyLocalStorageUpdated()
        val payload = SubscriptionPayload(
                username = getUsername()!!,
                endpoint = subscription.endpoint,
                key = subscription.getKey("p256dh")
                    .let { window.btoa(js("String").fromCharCode.apply(null, Uint8Array(it)) as String) },
                auth = subscription.getKey("auth")
                    .let { window.btoa(js("String").fromCharCode.apply(null, Uint8Array(it)) as String) }
            )

        jsonClient.post<Unit>(SERVER_URL + Routes.Subscription) {
            contentType(ContentType.Application.Json)
            body = payload
        }
    }

    override fun RBuilder.render() {
        flexBox {
            grades {  }
            if (state.pushManagerState == PushManagerState.NotSubscribed) {
                 div {
                    styledButton {
                        attrs {
                            onClickFunction = { subscribeUser() }
                        }
                        +"Subscribe to push notifications to receive a notification on this device upon receiving a mark."
                    }
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
