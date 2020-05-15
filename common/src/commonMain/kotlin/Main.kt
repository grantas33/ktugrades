package org.ktugrades.common

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

object Routes {
    const val Authenticate = "/authenticate"
    const val Grades = "/grades"
    const val Subscription = "/subscription"
    const val Unsubscription = "/unsubscription"
}

val jsonSerializer = Json(configuration = JsonConfiguration.Stable)

@Serializable
data class Credentials(val username: String, val password: String)

@Serializable
data class ErrorMessage(val message: String)

@Serializable
data class EncryptedUsername(val username: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EncryptedUsername

        if (!username.contentEquals(other.username)) return false

        return true
    }

    override fun hashCode(): Int {
        return username.contentHashCode()
    }
}

@Serializable
data class SubscriptionPayload(val username: ByteArray, val endpoint: String, val key: String, val auth: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SubscriptionPayload

        if (!username.contentEquals(other.username)) return false
        if (endpoint != other.endpoint) return false
        if (key != other.key) return false
        if (auth != other.auth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = username.contentHashCode()
        result = 31 * result + endpoint.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + auth.hashCode()
        return result
    }
}

@Serializable
data class UnsubscriptionPayload(val endpoint: String, val key: String, val auth: String)

@Serializable
class CommonDateTime(val millis: Long)

expect fun CommonDateTime.getFormatted(): String

@Serializer(forClass = CommonDateTime::class)
object CommonDateTimeSerializer

@Serializable
data class MarkInfoResponse(
    val moduleCode: String,
    val semesterCode: String,
    val title: String,
    val professor: String,
    val typeId: String?,
    val week: String,
    val date: CommonDateTime,
    val averageMarkForModule: Double?,
    val marks: List<String>
)

data class Module (
    val code: String,
    val semesterCode: String,
    val title: String,
    val professor: String
)

@Serializable
data class NotificationPayload(
    val addedMarks: List<MarkInfoResponse>,
    val updatedMarks: List<MarkInfoResponse>
)

fun List<String>.toMarkString() = this.joinToString(", ") { it.removePrefix("0") }

fun List<MarkInfoResponse>.sort() = this
    .sortedWith(compareByDescending<MarkInfoResponse> {it.date.millis}
        .thenByDescending {it.semesterCode }
        .thenByDescending {it.week.split("-").map {it.toInt()}.average()}
        .thenBy {it.title}
    )
