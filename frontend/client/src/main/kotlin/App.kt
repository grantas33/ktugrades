import kotlinx.coroutines.*
import org.w3c.dom.get
import react.*
import react.dom.h1
import kotlin.browser.localStorage
import kotlin.browser.window

interface AppState: RState {
    var serviceWorkerState: ServiceWorkerState
    var localStorageChangeSwitch: Boolean
}

class App: RComponent<RProps, AppState>() {

    override fun AppState.init() {
        serviceWorkerState = ServiceWorkerState.Loading
        localStorageChangeSwitch = false

        MainScope().launch {
            try {
                val swRegistration = window.navigator.serviceWorker.register("/serviceWorker.js").await()
                console.log("Successfully registered a service worker.", swRegistration)
                setState {
                    serviceWorkerState = ServiceWorkerState.Registered(swRegistration)
                }
            } catch (e: Exception) {
                console.warn(e.message)
                setState { serviceWorkerState = ServiceWorkerState.Failed }
            }
        }
    }

    override fun RBuilder.render() {
        flexBox {
            when (state.serviceWorkerState) {
                is ServiceWorkerState.Registered ->
                    if (localStorage["username"]?.length ?: 0 > 0 && localStorage["password"]?.length ?: 0 > 0) {
                        mainPage {
                            pushManager = (state.serviceWorkerState as ServiceWorkerState.Registered).swRegistration.pushManager
                        }
                    } else {
                        loginPage {
                            notifyLocalStorageUpdated = { setState { localStorageChangeSwitch = state.localStorageChangeSwitch.not()  } }
                        }
                    }
                ServiceWorkerState.Failed -> h1 {
                    +"Error in registering service worker"
                }
                ServiceWorkerState.Loading -> loadingComponent()
            }
        }
    }
}