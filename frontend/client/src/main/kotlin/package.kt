import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.ktugrades.common.ErrorMessage
import org.ktugrades.common.MarkInfoResponse
import org.w3c.workers.ServiceWorkerRegistration
import services.isKeyExistingInCache
import kotlin.browser.window
import kotlin.js.Promise

val PUSH_API_PUBLIC_KEY = "04:b4:64:c9:1a:7e:0e:b2:86:83:78:a0:97:92:bc:cb:84:72:20:b8:82:53:7c:bc:16:13:ab:ce:5d:91:c3:d6:7e:19:2c:c5:28:3b:73:69:1e:f2:a8:3f:7f:b1:9d:d8:85:e9:80:93:df:85:b9:c6:d6:a7:27:94:c0:2b:7d:bb:c0"
    .split(":")
    .let {
        Uint8Array(it.map { it.toInt(16).toByte() }.toTypedArray())
    }

const val SERVER_URL = "http://127.0.0.1:5000"

sealed class ServiceWorkerState {
    data class Registered(val swRegistration: ServiceWorkerRegistration): ServiceWorkerState()
    object Failed: ServiceWorkerState()
    object Loading: ServiceWorkerState()
}

enum class PushManagerState {
    Loading, Subscribed, NotSubscribed
}

data class PushSubscriptionOptions(val userVisibleOnly: Boolean, val applicationServerKey: Uint8Array)

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

suspend fun isCredentialsExisting() = window.caches.isKeyExistingInCache(DATA_CACHE,"username")

data class MarkType(val typeId: String?, val weeks: String)

sealed class MarkState {
    data class Success(val marks: List<MarkInfoResponse>): MarkState()
    data class Error(val error: ErrorMessage): MarkState()
    object Loading: MarkState()
}
