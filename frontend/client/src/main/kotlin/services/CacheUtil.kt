package services

import DATA_CACHE
import kotlinx.coroutines.await
import org.w3c.fetch.Response
import kotlin.browser.window

suspend fun putDataInCache(key: String, data: dynamic) {
    val cache = window.caches.open(DATA_CACHE).await()
    cache.put(key, Response(body = data))
}

suspend fun getDataTextInCache(key: String): String? {
    val cache = window.caches.open(DATA_CACHE).await()
    return cache.match(key).await()?.unsafeCast<Response>()?.text()?.await()
}

suspend fun isKeyExisting(key: String): Boolean {
    val cache = window.caches.open(DATA_CACHE).await()
    return cache.match(key).await() != null
}
