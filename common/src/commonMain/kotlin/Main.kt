package org.ktugrades.common

data class Credentials(val username: String, val password: String)

data class ErrorMessage(val message: String)

expect class AuthenticationResponse(username: ByteArray, password: ByteArray)

data class SubscriptionPayload(val endpoint: String, val key: String, val auth: String)
