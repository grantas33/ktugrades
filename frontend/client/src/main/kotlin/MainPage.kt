import components.*
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.ktugrades.common.SubscriptionPayload
import kotlinx.coroutines.*
import kotlinx.css.*
import kotlinx.html.js.onClickFunction
import org.ktugrades.common.Routes
import react.*
import react.dom.div
import services.getUsername
import styled.css
import kotlin.browser.window

interface MainPageProps: RProps, CacheProps {
    var pushManager: PushManager?
}
interface MainPageState: RState {
    var pushManagerState: PushManagerState
}

class MainPage: RComponent<MainPageProps, MainPageState>() {

    override fun MainPageState.init() {
        MainScope().launch {
            if (props.pushManager != null) {
                pushManagerState = PushManagerState.Loading
                props.pushManager!!.getSubscription().await().let {
                    setState {
                        pushManagerState = if (it != null) PushManagerState.Subscribed else PushManagerState.NotSubscribed
                    }
                }
            } else {
                pushManagerState = PushManagerState.NotSupported
            }
        }
    }

    private fun subscribeUser() = GlobalScope.launch {
        try {
            val subscription = props.pushManager!!.subscribe(
                PushSubscriptionOptions(userVisibleOnly = true, applicationServerKey = PUSH_API_PUBLIC_KEY)
            ).await()
            sendSubscriptionToServer(subscription)
            console.log("Subscribed")
            setState {
                pushManagerState = PushManagerState.Subscribed
            }
        } catch (e: Exception) {
            console.warn("Subscription denied - ${e.message}")
        }
    }

    private suspend fun sendSubscriptionToServer(subscription: PushSubscription) {
        if (!isCredentialsExisting()) props.setCredentialsExisting(false)
        val payload = SubscriptionPayload(
                username = window.caches.getUsername()!!,
                endpoint = subscription.endpoint,
                key = subscription.getKeyForRequest(),
                auth = subscription.getAuthForRequest()
            )

        jsonClient.post<Unit>(SERVER_URL + Routes.Subscription) {
            contentType(ContentType.Application.Json)
            body = payload
        }
    }

    override fun RBuilder.render() {
        child(functionalComponent = Grades)
        if (state.pushManagerState != PushManagerState.Loading) {
            child(
                functionalComponent = Logout,
                props = PushManagerState.Subscribed.let { subscribedState ->
                    jsObject {
                        pushManager = if (state.pushManagerState == subscribedState) props.pushManager else null
                        setCredentialsExisting = props.setCredentialsExisting
                    }
                }
            )
        }
        if (state.pushManagerState == PushManagerState.NotSubscribed) {
            div {
                appButton {
                    css {
                        position = Position.fixed
                        bottom = LinearDimension("0")
                        left = LinearDimension("0")
                        width = LinearDimension("100%")
                        height = LinearDimension("60px")
                        background = Color.white.value
                        mobileView {
                            height = LinearDimension("20%")
                            fontSize = LinearDimension("6vw")
                        }
                    }
                    attrs {
                        onClickFunction = { subscribeUser() }
                    }
                    +"Subscribe to push notifications to receive a notification on this device upon receiving a mark."
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
