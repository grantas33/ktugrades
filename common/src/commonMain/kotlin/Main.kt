package org.ktugrades.common

data class Credentials(val username: String, val password: String)

data class ErrorMessage(val message: String)

data class AuthenticationResponse(val username: ByteArray, val password: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AuthenticationResponse

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
