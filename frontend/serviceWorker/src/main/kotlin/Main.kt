import kotlinx.coroutines.*
import kotlinx.serialization.builtins.list
import org.ktugrades.common.*
import org.w3c.fetch.Request
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import org.w3c.fetch.ResponseInit
import org.w3c.notifications.NotificationEvent
import org.w3c.notifications.NotificationOptions
import org.w3c.workers.*
import services.getTextInCache
import services.getUsername
import kotlin.js.Promise

external val self: ServiceWorkerGlobalScope

fun main() {
    installServiceWorker()
}

const val MAIN_CACHE = "mainCache"
const val FETCH_CACHE = "fetchCache"

fun installServiceWorker() {
    val offlineContent = arrayOf(
            "/index.html",
            "/main.bundle.js",
            "/ktu-ikona.png",
            "manifest.webmanifest",
            "PfdintextproMedium.ttf"
    )

    self.addEventListener("install", { event ->
        event as InstallEvent
        console.log("I am installed.")
        event.waitUntil(
                self.caches.open(MAIN_CACHE).then {
                    it.addAll(offlineContent)
                }.then {
                    console.log("Offline cache loaded.")
                }
        )
    })

    self.addEventListener("fetch",  { event ->
        event as FetchEvent
        if (event.request.url.contains("http").not()) return@addEventListener
        fun fetchPromise(cache: Cache): Promise<Response> = self.fetch(event.request).then {
            cache.put(event.request, it.clone())
            it
        }.catch {
            return@catch self.caches.match(event.request).unsafeCast<Response>()
        }

        event.respondWith(
            self.caches.open(FETCH_CACHE).then { cache ->
                return@then fetchPromise(cache).unsafeCast<Response>()
            }
        )
    });

    self.addEventListener("push", { event ->
        event as PushEvent
        console.log("Push received.")
        val payloadString = event.data.text()
        val notificationPayload = jsonSerializer.parse(NotificationPayload.serializer(), payloadString)
        val message = notificationPayload.run {
            when {
                addedMarks.size + updatedMarks.size > 1 -> "You received ${addedMarks.size + updatedMarks.size} new marks!"
                addedMarks.size == 1 -> "You received a new mark ${addedMarks.first().marks.toMarkString()}" +
                    (addedMarks.first().typeId?.let { " for $it" } ?: "") +
                    " in ${addedMarks.first().title}."
                updatedMarks.size == 1 -> "Your mark" + (updatedMarks.first().typeId?.let { " for $it" } ?: "") +
                    " in ${updatedMarks.first().title} got updated to ${updatedMarks.first().marks.toMarkString()}"
                else -> null
            }
        }

        MainScope().launch {
            val username = requireNotNull(self.caches.getUsername())
            val serializedUsername = jsonSerializer.stringify(EncryptedUsername.serializer(), EncryptedUsername(username = username))
            val request = "${SERVER_URL}${Routes.Grades}?username=${encodeURIComponent(serializedUsername)}"
            val requestObj = Request(request, RequestInit(headers = applicationJsonHeaders))
            val cachedMarksString = requireNotNull(self.caches.getTextInCache(FETCH_CACHE, requestObj))
            val cachedMarks = jsonSerializer.parse(MarkInfoResponse.serializer().list, cachedMarksString)
            val updatedMarks = (cachedMarks + notificationPayload.addedMarks + notificationPayload.updatedMarks).sort()
            self.caches.open(FETCH_CACHE).await().put(
                request = requestObj,
                response = Response(
                    body = jsonSerializer.stringify(MarkInfoResponse.serializer().list, updatedMarks),
                    init = ResponseInit(headers = applicationJsonHeaders)
                )
            )
        }

        if (message != null) {
            event.waitUntil(
                self.registration.showNotification(
                    title = "KTU grades",
                    options = NotificationOptions(
                        tag = "tag",
                        body = message,
                        icon = "/ktu-ikona.png",
                        badge = "/ktu-ikona.png"
                    )
                )
            )
        }
    })

    self.addEventListener("notificationclick", { event ->
        event as NotificationEvent
        console.log("Notification click received.")
        event.notification.close()
        event.waitUntil(
            self.clients.openWindow("localhost:8080")
        )
    })
}
