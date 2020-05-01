package org.ktugrades.backend.services

import kotlinx.serialization.json.Json
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.ktugrades.backend.getVapidKeyPair
import org.ktugrades.backend.handlers.LoginHandler
import org.ktugrades.backend.toResponse
import org.ktugrades.backend.withCoroutineClient
import org.ktugrades.common.NotificationPayload
import java.lang.RuntimeException

class NotificationService(
    private val mySqlProvider: MySqlProvider,
    private val credentialProvider: CredentialProvider,
    private val loginHandler: LoginHandler,
    private val markService: MarkService,
    private val jsonSerializer: Json
) {

    suspend fun broadcastToAll(onSingleUserError: (e: java.lang.Exception) -> Unit) {
        val vapidKeyPair = getVapidKeyPair() ?: throw RuntimeException("Could not get VAPID key pair.")
        val pushService = PushService().apply {
            publicKey = vapidKeyPair.public
            privateKey = vapidKeyPair.private
        }

        val usersWithSubscriptions = mySqlProvider.getUsersWithSubscriptions()

        usersWithSubscriptions.forEach {
            try {
                val credentials = credentialProvider.getCredentials(it.user.username, it.user.password)
                withCoroutineClient {
                    loginHandler.getAuthCookie(credentials.username, credentials.password)
                    val markAggregationResult = markService.getMarks(it.user.username)
                    markAggregationResult.run {
                        mySqlProvider.insertModules(newModules)
                        mySqlProvider.insertMarks(marks = markInfoToAddAndNotify, user = it.user.username)
                        mySqlProvider.updateMarks(markInfoToUpdate + markInfoToUpdateAndNotify, it.user.username)
                        if (markInfoToAddAndNotify.isNotEmpty() || markInfoToUpdateAndNotify.isNotEmpty()) {
                            val notificationPayload = NotificationPayload(
                                addedMarks = markInfoToAddAndNotify.map { it.toResponse() },
                                updatedMarks = markInfoToUpdateAndNotify.map { it.toResponse() },
                                latestMarks = latestMarks.map {it.toResponse() }
                            )
                            it.subscriptions.forEach {
                                pushService.send(
                                    Notification(
                                        it.endpoint,
                                        it.publicKey,
                                        it.auth,
                                        jsonSerializer.stringify(NotificationPayload.serializer(), notificationPayload)
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                onSingleUserError(e)
            }
        }
    }
}