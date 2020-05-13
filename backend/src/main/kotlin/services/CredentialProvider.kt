package org.ktugrades.backend.services

import org.ktugrades.backend.User
import org.ktugrades.common.Credentials

typealias CredentialUserProvider = suspend (encryptedUsername: ByteArray) -> User

class CredentialProvider(
    private val userProvider: CredentialUserProvider,
    private val encryptionService: EncryptionService
) {
    suspend fun getCredentials(encryptedUsername: ByteArray): Credentials {
        return userProvider(encryptedUsername).let {
            Credentials(
                username = encryptionService.decrypt(it.username),
                password = encryptionService.decrypt(it.password)
            )
        }
    }

    fun getCredentials(encryptedUsername: ByteArray, encryptedPassword: ByteArray) = Credentials(
        username = encryptionService.decrypt(encryptedUsername),
        password = encryptionService.decrypt(encryptedPassword)
    )
}