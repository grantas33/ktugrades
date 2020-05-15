package org.ktugrades.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.cookies.AcceptAllCookiesStorage
import io.ktor.client.features.cookies.HttpCookies
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.joda.time.DateTime
import org.ktugrades.common.CommonDateTime
import org.ktugrades.common.MarkInfoResponse
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.security.KeyPair
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

fun getVapidKeyPair(): KeyPair? {
    val reader = InputStreamReader(object {}.javaClass.getResourceAsStream("/vapid_private.pem"))
    val pemParser = PEMParser(reader)
    val keyPair = pemParser.readObject() as PEMKeyPair

    return JcaPEMKeyConverter().getKeyPair(keyPair)
}

data class CoroutineClient(
    val client: HttpClient
) : AbstractCoroutineContextElement(CoroutineClient) {
    companion object Key : CoroutineContext.Key<CoroutineClient>
}

suspend fun <T> withCoroutineClient(block: suspend CoroutineScope.() -> T): T {
    @Suppress("EXPERIMENTAL_API_USAGE")
    val client = CoroutineClient(
        client = HttpClient(CIO) {
            install(JsonFeature) { serializer = GsonSerializer() }
            install(HttpCookies) { storage = AcceptAllCookiesStorage() }
        }
    )

    return withContext(client) {
        block()
    }
}

suspend fun getClient() = coroutineContext[CoroutineClient]!!.client

data class User(val username: ByteArray, val password: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (!username.contentEquals(other.username)) return false
        if (!password.contentEquals(other.password)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.contentHashCode()
        result = 31 * result + password.contentHashCode()
        return result
    }
}

data class MarkAggregationResult(
    val markInfoToAddAndNotify: List<MarkInfo>,
    val markInfoToUpdateAndNotify: List<MarkInfo>,
    val markInfoToUpdate: List<MarkInfo>,
    val latestMarks: List<MarkInfoResponse>
)

data class MarkInfo(
    val id: UUID,
    val moduleCode: String,
    val semesterCode: String,
    val title: String,
    val professor: String,
    val typeId: String?,
    val week: String,
    val date: DateTime,
    val averageMarkForModule: Double?,
    val marks: List<String>
)

fun MarkInfo.toResponse(): MarkInfoResponse = MarkInfoResponse(
    moduleCode = moduleCode,
    semesterCode = semesterCode,
    title = title,
    professor = professor,
    typeId = typeId,
    week = week,
    date = CommonDateTime(this.date.millis),
    averageMarkForModule = averageMarkForModule,
    marks = marks
)

data class MarkSlot(
    val moduleCode: String,
    val semesterCode: String,
    val typeId: String?,
    val week: String,
    val averageMark: Double?
)

fun UUID.getBytes(): ByteArray = ByteBuffer.wrap(ByteArray(16)).let {
    it.putLong(this.mostSignificantBits)
    it.putLong(this.leastSignificantBits)
}.array()

fun ByteArray.getUUID(): UUID = ByteBuffer.wrap(this).run { UUID(long, long) }
fun String.toFixedByteArray(size: Int) = this.toByteArray().plus(ByteArray(size = size - this.length))

data class UserSubscription(val id: UUID, val userId: UUID, val endpoint: String, val publicKey: String, val auth: String)
data class UserSubscriptionData(val user: User, val subscriptions: List<SubscriptionData>)

data class SubscriptionData(val endpoint: String, val publicKey: String, val auth: String)
