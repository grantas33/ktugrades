package org.ktugrades.backend.services

import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.util.length
import org.joda.time.LocalDateTime
import org.ktugrades.backend.*
import org.ktugrades.common.Module
import org.ktugrades.common.SubscriptionPayload
import org.ktugrades.common.UnsubscriptionPayload
import java.lang.RuntimeException
import java.util.*

class MySqlProvider(val connection: SuspendingConnection, private val queryProvider: MySqlQueryProvider) {
    suspend fun getUser(username: ByteArray): User = connection.inTransaction {
        queryProvider.getUser(username = username, conn = it)
    }

    suspend fun upsertUser(username: ByteArray, password: ByteArray) = connection.inTransaction {
        queryProvider.upsertUser(username = username, password = password, conn = it)
    }

    suspend fun insertUserSubscription(payload: SubscriptionPayload) = connection.inTransaction {
        queryProvider.insertUserSubscription(payload = payload, conn = it)
    }

    suspend fun deleteUserSubscription(payload: UnsubscriptionPayload) = connection.inTransaction {
        queryProvider.deleteUserSubscription(payload = payload, conn = it)
    }

    suspend fun getUsersWithSubscriptions(): List<UserSubscriptionData> = connection.inTransaction {
        queryProvider.getUsersWithSubscriptions(conn = it)
    }

    suspend fun getMarksForUser(username: ByteArray): List<MarkInfo> = connection.inTransaction {
        queryProvider.getMarksForUser(username = username, conn = it)
    }

    suspend fun insertNewMarkInformation(marks: List<MarkInfo>, user: ByteArray) = connection.inTransaction {
        queryProvider.insertNewMarkInformation(marks = marks, user = user, conn = it)
    }

    suspend fun updateMarks(marks: List<MarkInfo>, user: ByteArray) = connection.inTransaction {
        queryProvider.updateMarks(marks = marks, user = user, conn = it)
    }

    suspend fun getAverageMarks(): List<MarkSlot> = connection.inTransaction {
        queryProvider.getAverageMarks(conn = it)
    }

    suspend fun insertMarkSlotAverages(markSlots: List<MarkSlot>) = connection.inTransaction {
        queryProvider.insertMarkSlotAverages(markSlots = markSlots, conn = it)
    }

}

class MySqlQueryProvider {

    suspend fun getUser(username: ByteArray, conn: SuspendingConnection): User = conn.sendPreparedStatement(
        query = """
                select * from User
                where User.username = ?
            """.trimIndent(),
        values = listOf(username)
    ).let {
        if (it.rows.length > 1) throw RuntimeException("Multiple users found with the same username.")
        if (it.rows.length < 1) throw IllegalArgumentException("User not found.")
        it.rows[0].let {
            User(
                username = it["username"] as ByteArray,
                password = it["password"] as ByteArray
            )
        }
    }


    suspend fun upsertUser(username: ByteArray, password: ByteArray, conn: SuspendingConnection) = conn.sendPreparedStatement(
        query = """
                insert into User (username, password) 
                values (?, ?)
                on duplicate key update 
                password = ?
            """.trimIndent(),
        values = listOf(username, password, password)
    )

    suspend fun insertUserSubscription(payload: SubscriptionPayload, conn: SuspendingConnection) = conn.sendPreparedStatement(
        query = """
                insert into UserSubscriptions (id, userId, endpoint, publicKey, auth) 
                values (?, ?, ?, ?, ?)
            """.trimIndent(),
        values = listOf(UUID.randomUUID().getBytes(), payload.username, payload.endpoint, payload.key, payload.auth)
    )


    suspend fun deleteUserSubscription(payload: UnsubscriptionPayload, conn: SuspendingConnection) = conn.sendPreparedStatement(
        query = """
                delete from UserSubscriptions where endpoint = ? and publicKey = ? and auth = ?
            """.trimIndent(),
        values = listOf(payload.endpoint, payload.key, payload.auth)
    )


    suspend fun getUsersWithSubscriptions(conn: SuspendingConnection): List<UserSubscriptionData> = conn.sendPreparedStatement(
        query = """
                select * from User
                join UserSubscriptions on User.username = UserSubscriptions.userId
            """.trimIndent()
    ).let {
        it.rows.groupBy { User(username = it["username"] as ByteArray, password = it["password"] as ByteArray) }
            .map {
                UserSubscriptionData(
                    user = it.key,
                    subscriptions = it.value.map {
                        SubscriptionData(
                            endpoint = it["endpoint"] as String,
                            publicKey = it["publicKey"] as String,
                            auth = it["auth"] as String
                        )
                    }
                )
            }
    }

    suspend fun getMarksForUser(username: ByteArray, conn: SuspendingConnection): List<MarkInfo> = conn.sendPreparedStatement(
        query = """
                select MarkInformation.*, Module.title, Module.professor, MarkSlot.averageMark, group_concat(Mark.mark) as `marks` from MarkInformation
                join MarkSlot on MarkInformation.moduleCode = MarkSlot.moduleCode 
                    and MarkInformation.semesterCode = MarkSlot.semesterCode
                    and MarkInformation.typeId = MarkSlot.typeId
                    and MarkInformation.week = MarkSlot.week
                join Module on MarkSlot.moduleCode = Module.code and MarkSlot.semesterCode = Module.semesterCode
                join Mark on MarkInformation.id = Mark.markInformationId
                where MarkInformation.userId = ?
                group by MarkInformation.id
            """.trimIndent(),
        values = listOf(username)
    ).let {
        it.rows.map {
            MarkInfo(
                id = (it["id"] as ByteArray).getUUID(),
                moduleCode = it["moduleCode"] as String,
                semesterCode = it["semesterCode"] as String,
                title = it["title"] as String,
                professor = it["professor"] as String,
                typeId = it["typeId"] as String?,
                week = it["week"] as String,
                date = (it["date"] as LocalDateTime).toDateTime(),
                averageMarkForModule = it["averageMark"] as Double?,
                marks = (it["marks"] as String).split(",")
            )
        }
    }

    suspend fun insertNewMarkInformation(marks: List<MarkInfo>, user: ByteArray, conn: SuspendingConnection) {
        if (marks.isNotEmpty()) {
            val modules = marks.map { Module(code = it.moduleCode, semesterCode = it.semesterCode, title = it.title, professor = it.professor) }
                .distinct()
            conn.sendPreparedStatement(
                query = """
                insert into Module (code, semesterCode, title, professor) 
                values ${modules.joinToString(", ") { "(?, ?, ?, ?)" }}
                on duplicate key update code=code, semesterCode=semesterCode
            """.trimIndent(),
                values = modules.map { listOf(it.code, it.semesterCode, it.title, it.professor) }.flatten()
            )

            val markSlots = marks.map { MarkSlot(it.moduleCode, it.semesterCode, it.typeId, it.week, null) }
                .distinct()
            conn.sendPreparedStatement(
                query = """
                insert into MarkSlot (moduleCode, semesterCode, typeId, week, averageMark) 
                values ${markSlots.joinToString(", ") { "(?, ?, ?, ?, ?)" }}
                on duplicate key update moduleCode=moduleCode, semesterCode=semesterCode, typeId=typeId, week=week
            """.trimIndent(),
                values = markSlots.map { listOf(it.moduleCode, it.semesterCode, it.typeId, it.week, it.averageMark) }.flatten()
            )

            conn.sendPreparedStatement(
                query = """
                insert into MarkInformation (id, moduleCode, semesterCode, userId, typeId, week, date) 
                values ${marks.joinToString(", ") { "(?, ?, ?, ?, ?, ?, ?)" }}
        """.trimIndent(),
                values = marks.map { listOf(it.id.getBytes(), it.moduleCode, it.semesterCode, user, it.typeId, it.week, it.date) }.flatten()
            )

            conn.sendPreparedStatement(
                query = """
                insert into Mark (id, markInformationId, mark) 
                values ${marks.flatMap { it.marks }.joinToString(", ") { "(?, ?, ?)" }}
        """.trimIndent(),
                values = marks.map { markInfo -> markInfo.marks.map { listOf(UUID.randomUUID().getBytes(), markInfo.id.getBytes(), it) }.flatten() }.flatten()
            )
        }
    }

    suspend fun updateMarks(marks: List<MarkInfo>, user: ByteArray, conn: SuspendingConnection) {
        if (marks.isNotEmpty()) {
            conn.sendPreparedStatement(
                query = """
                insert into MarkInformation (id, moduleCode, semesterCode, userId, typeId, week, date) 
                values ${marks.joinToString(", ") { "(?, ?, ?, ?, ?, ?, ?)" }}
                on duplicate key update date=values(date)
            """.trimIndent(),
                values = marks.map { listOf(it.id.getBytes(), it.moduleCode, it.semesterCode, user, it.typeId, it.week, it.date) }.flatten()
            )

            conn.sendPreparedStatement(
                query = """
                delete from Mark where markInformationId in (${marks.joinToString(", ") { "?" }})
            """.trimIndent(),
                values = marks.map { it.id.getBytes() }
            )

            conn.sendPreparedStatement(
                query = """
                insert into Mark (id, markInformationId, mark) 
                values ${marks.flatMap { it.marks }.joinToString(", ") { "(?, ?, ?)" }}
            """.trimIndent(),
                values = marks.map { markInfo -> markInfo.marks.map { listOf(UUID.randomUUID().getBytes(), markInfo.id.getBytes(), it) }.flatten() }.flatten()
            )
        }
    }

    suspend fun getAverageMarks(conn: SuspendingConnection): List<MarkSlot> = conn.sendPreparedStatement(
        query = """
                select MarkInformation.moduleCode, MarkInformation.semesterCode, MarkInformation.typeId, MarkInformation.week, avg(Mark.mark) as `avg`
                from MarkInformation join Mark on MarkInformation.id = Mark.markInformationId
                where Mark.mark REGEXP '^[0-9]+$'
                group by MarkInformation.moduleCode, MarkInformation.semesterCode, MarkInformation.typeId, MarkInformation.week
            """.trimIndent()
    ).let {
        it.rows.map {
            MarkSlot(
                moduleCode = it["moduleCode"] as String,
                semesterCode = it["semesterCode"] as String,
                typeId = it["typeId"] as String?,
                week = it["week"] as String,
                averageMark = it["avg"] as Double?
            )
        }
    }

    suspend fun insertMarkSlotAverages(markSlots: List<MarkSlot>, conn: SuspendingConnection) = conn.sendPreparedStatement(
        query = """
                insert into MarkSlot (moduleCode, semesterCode, typeId, week, averageMark) 
                values ${markSlots.joinToString(", ") { "(?, ?, ?, ?, ?)" }}
                on duplicate key update averageMark=values(averageMark)
            """.trimIndent(),
        values = markSlots.map { listOf(it.moduleCode, it.semesterCode, it.typeId, it.week, it.averageMark) }.flatten()
    )
}