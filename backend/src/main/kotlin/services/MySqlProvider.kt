package org.ktugrades.backend.services

import com.github.jasync.sql.db.SuspendingConnection
import org.ktugrades.common.SubscriptionPayload

class MySqlProvider(val connection: SuspendingConnection) {

    suspend fun upsertUser(username: ByteArray, password: ByteArray) = connection.inTransaction {
        it.sendPreparedStatement(
            query = """
                insert into User (username, password) 
                values (?, ?)
                on duplicate key update
                password = ?
            """.trimIndent(),
            values = listOf(username, password, password)
        )
    }

    suspend fun insertUserSubscription(payload: SubscriptionPayload) = connection.inTransaction {
        it.sendPreparedStatement(
            query = """
                insert into UserSubscriptions (userId, endpoint, publicKey, auth) 
                values (?, ?, ?, ?)
            """.trimIndent(),
            values = listOf(payload.username, payload.endpoint, payload.key, payload.auth)
        )
    }
}