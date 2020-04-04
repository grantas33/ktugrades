package org.ktugrades.common

actual class AuthenticationResponse actual constructor(val username: ByteArray, val password: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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
