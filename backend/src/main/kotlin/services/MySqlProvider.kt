package org.ktugrades.backend.services

import com.github.jasync.sql.db.SuspendingConnection

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
}