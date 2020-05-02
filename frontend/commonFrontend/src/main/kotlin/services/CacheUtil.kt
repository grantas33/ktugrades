package services

import DATA_CACHE
import kotlinx.coroutines.await
import org.w3c.fetch.Response
import org.w3c.workers.CacheStorage

suspend fun CacheStorage.putInCache(cache: String, key: Any, data: dynamic) {
    this.open(cache).await().put(key, Response(body = data))
}

suspend fun CacheStorage.getTextInCache(cache: String, key: Any): String? {
    return this.open(cache).await().match(key).await()?.unsafeCast<Response>()?.text()?.await()
}

suspend fun CacheStorage.isKeyExistingInCache(cache: String, key: String): Boolean {
    return this.open(cache).await().match(key).await() != null
}

suspend fun CacheStorage.getUsername(): ByteArray? = getTextInCache(DATA_CACHE,"username")?.let { JSON.parse(it) }

