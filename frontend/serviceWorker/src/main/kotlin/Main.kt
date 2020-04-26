import org.w3c.fetch.Response
import org.w3c.notifications.NotificationEvent
import org.w3c.notifications.NotificationOptions
import org.w3c.workers.*
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
        val message = event.data.text()
        console.log("Push had this data: $message")

        if (message != "12") {
            event.waitUntil(
                    self.registration.showNotification(
                            title = message,
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
