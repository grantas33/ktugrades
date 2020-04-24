package org.ktugrades.backend.services

import org.ktugrades.backend.User
import org.ktugrades.common.Credentials

class CredentialProvider(
    private val userProvider: suspend (encryptedUsername: ByteArray) -> User,
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
}