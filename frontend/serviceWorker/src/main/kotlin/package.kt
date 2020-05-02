import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import org.w3c.workers.ExtendableEvent
import org.w3c.workers.ExtendableEventInit

external interface PushMessageData {
    fun arrayBuffer(): ArrayBuffer
    fun blob(): Blob
    fun json(): JSON
    fun text(): String
}

/**
 * Exposes the JavaScript [PushEvent](https://developer.mozilla.org/en-US/docs/Web/API/PushEvent) to Kotlin
 */
open external class PushEvent(type: String, eventInitDict: ExtendableEventInit = definedExternally) : ExtendableEvent {
    val data: PushMessageData
}

const val SERVER_URL = "http://127.0.0.1:5000"

inline fun jsObject(init: dynamic.() -> Unit): dynamic {
    val o = js("{}")
    init(o)
    return o
}

val applicationJsonHeaders = jsObject { this["Content-type"] = "application/json" }

external fun encodeURIComponent(component: String): String