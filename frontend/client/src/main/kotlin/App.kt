import components.flexBox
import components.loadingComponent
import kotlinx.coroutines.*
import react.*
import react.dom.h1
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

    private fun onLocalStorageUpdate() {
        setState { localStorageChangeSwitch = state.localStorageChangeSwitch.not() }
    }

    override fun RBuilder.render() {
        flexBox {
            when (state.serviceWorkerState) {
                is ServiceWorkerState.Registered ->
                    if (isCredentialsExisting()) {
                        mainPage {
                            pushManager = (state.serviceWorkerState as ServiceWorkerState.Registered).swRegistration.pushManager
                            notifyLocalStorageUpdated = { onLocalStorageUpdate() }
                        }
                    } else {
                        loginPage {
                            notifyLocalStorageUpdated = { onLocalStorageUpdate() }
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