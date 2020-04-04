import org.khronos.webgl.ArrayBuffer
import org.w3c.workers.ServiceWorkerRegistration
import kotlin.js.Promise

const val PUSH_API_PUBLIC_KEY = "BF2T7idzBuyRT3i6RhfObqIW0DbSC7vnbFV6GRyDBq39pO6VL9LMw3yp3iQbzxQagbcSBomrUBQk7qzFMT_bM94"
const val SERVER_URL = "http://127.0.0.1:5000"

sealed class ServiceWorkerState {
    data class Registered(val swRegistration: ServiceWorkerRegistration): ServiceWorkerState()
    object Failed: ServiceWorkerState()
    object Loading: ServiceWorkerState()
}

enum class PushManagerState {
    Loading, Subscribed, NotSubscribed
}

data class PushSubscriptionOptions(val userVisibleOnly: Boolean, val applicationServerKey: IntArray)

external class PushSubscription {
    val endpoint: String
    val expirationTime: dynamic
    val options: PushSubscriptionOptions

    fun getKey(name: String): ArrayBuffer
    fun toJSON(): JSON
    fun unsubscribe(): Promise<Boolean>
}

inline val ServiceWorkerRegistration.pushManager: PushManager
    get() = asDynamic().pushManager as PushManager

/**
 * Exposes the JavaScript [PushManager](https://developer.mozilla.org/en-US/docs/Web/API/PushManager) to Kotlin
 */
external class PushManager {
    fun getSubscription(): Promise<PushSubscription?>
    fun permissionState(): Promise<String>
    fun subscribe(options: PushSubscriptionOptions): Promise<PushSubscription>
}

inline fun jsObject(init: dynamic.() -> Unit): dynamic {
    val o = js("{}")
    init(o)
    return o
}

val applicationJsonHeaders = jsObject { this["Content-type"] = "application/json" }
