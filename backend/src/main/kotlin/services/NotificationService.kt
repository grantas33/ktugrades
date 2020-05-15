package org.ktugrades.backend.services

import kotlinx.serialization.json.Json
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.ktugrades.backend.UserSubscriptionData
import org.ktugrades.backend.getVapidKeyPair
import org.ktugrades.backend.handlers.LoginHandler
import org.ktugrades.backend.toResponse
import org.ktugrades.backend.withCoroutineClient
import org.ktugrades.common.NotificationPayload
import java.lang.RuntimeException

typealias ErrorAction = (e: java.lang.Exception) -> Unit

class NotificationService(
    private val mySqlProvider: MySqlProvider,
    private val credentialProvider: CredentialProvider,
    private val loginHandler: LoginHandler,
    private val markService: MarkService,
    private val jsonSerializer: Json
) {

    suspend fun broadcastToAll(onSingleUserError: ErrorAction) {
        val vapidKeyPair = getVapidKeyPair() ?: throw RuntimeException("Could not get VAPID key pair.")
        val pushService = PushService().apply {
            publicKey = vapidKeyPair.public
            privateKey = vapidKeyPair.private
        }

        val usersWithSubscriptions = mySqlProvider.getUsersWithSubscriptions()

        usersWithSubscriptions.forEach {
            try {
                sendNotificationToUserSubscriptions(it, pushService)
            } catch (e: Exception) {
                onSingleUserError(e)
            }
        }
    }

    suspend fun sendNotificationToUserSubscriptions(data: UserSubscriptionData, pushService: PushService) {
        val credentials = credentialProvider.getCredentials(data.user.username, data.user.password)
        withCoroutineClient {
            loginHandler.getAuthCookie(credentials.username, credentials.password)
            val markAggregationResult = markService.getMarks(data.user.username)
            markAggregationResult.run {
                mySqlProvider.insertNewMarkInformation(marks = markInfoToAddAndNotify, user = data.user.username)
                mySqlProvider.updateMarks(markInfoToUpdate + markInfoToUpdateAndNotify, data.user.username)
                if (markInfoToAddAndNotify.isNotEmpty() || markInfoToUpdateAndNotify.isNotEmpty()) {
                    val notificationPayload = NotificationPayload(
                        addedMarks = markInfoToAddAndNotify.map { it.toResponse() },
                        updatedMarks = markInfoToUpdateAndNotify.map { it.toResponse() }
                    )
                    data.subscriptions.forEach {
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
    }
}