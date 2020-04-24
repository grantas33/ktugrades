package org.ktugrades.backend.services

import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.ktugrades.backend.getVapidKeyPair
import java.lang.RuntimeException

class NotificationService(private val mySqlProvider: MySqlProvider) {

    suspend fun broadcastToAll() {
        val vapidKeyPair = getVapidKeyPair() ?: throw RuntimeException("Could not get VAPID key pair.")
        val pushService = PushService().apply {
            publicKey = vapidKeyPair.public
            privateKey = vapidKeyPair.private
        }
        val subscriptions = mySqlProvider.getUserSubscriptions()
        subscriptions.forEach {
            val res = pushService.send(
                Notification(
                    it.endpoint,
                    it.key,
                    it.auth,
                    "hello its the masrshian space jam is the artist"
                )
            )
        }
    }

    suspend fun sendPushNotificationToUser(user: ByteArray) {

    }
}