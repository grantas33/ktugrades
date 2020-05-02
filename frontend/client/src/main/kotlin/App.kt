import components.flexBox
import components.loadingComponent
import kotlinx.coroutines.*
import react.*
import react.dom.h1
import kotlin.browser.window

interface AppState: RState {
    var serviceWorkerState: ServiceWorkerState
    var isCredentialsExisting: Boolean
}

class App: RComponent<RProps, AppState>() {

    override fun AppState.init() {
        serviceWorkerState = ServiceWorkerState.Loading

        MainScope().launch {
            val swOp = async {
                try {
                    val swRegistration = window.navigator.serviceWorker.register("/serviceWorker.js").await()
                    console.log("Successfully registered a service worker.", swRegistration)
                    ServiceWorkerState.Registered(swRegistration)
                } catch (e: Exception) {
                    console.warn(e.message)
                    ServiceWorkerState.Failed
                }
            }
            val credentialOp = async { isCredentialsExisting() }

            val swState = swOp.await()
            val isCredentialsExist = credentialOp.await()

            setState {
                serviceWorkerState = swState
                isCredentialsExisting = isCredentialsExist
            }
        }
    }

    private fun setCredentialsExist(isCredentialsExist: Boolean) {
        setState { isCredentialsExisting = isCredentialsExist }
    }

    override fun RBuilder.render() {
        flexBox {
            when (state.serviceWorkerState) {
                is ServiceWorkerState.Registered ->
                    if (state.isCredentialsExisting) {
                        mainPage {
                            pushManager = (state.serviceWorkerState as ServiceWorkerState.Registered).swRegistration.pushManager
                            setCredentialsExisting = { setCredentialsExist(it) }
                        }
                    } else {
                        loginPage {
                            setCredentialsExisting = { setCredentialsExist(it) }
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