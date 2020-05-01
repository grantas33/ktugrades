package org.ktugrades.backend.services

import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.util.length
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.ktugrades.backend.*
import org.ktugrades.common.Module
import org.ktugrades.common.SubscriptionPayload
import java.lang.RuntimeException
import java.util.*

class MySqlProvider(val connection: SuspendingConnection) {

    suspend fun getUser(username: ByteArray): User = connection.inTransaction {
        it.sendPreparedStatement(
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
    }

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
                insert into UserSubscriptions (id, userId, endpoint, publicKey, auth) 
                values (?, ?, ?, ?, ?)
            """.trimIndent(),
            values = listOf(UUID.randomUUID().getBytes(), payload.username, payload.endpoint, payload.key, payload.auth)
        )
    }

    suspend fun getUserSubscriptions(): List<SubscriptionPayload> = connection.inTransaction {
        it.sendPreparedStatement(
            query = """
                select * from UserSubscriptions
            """.trimIndent()
        ).let {
            it.rows.map {
                SubscriptionPayload(
                    username = it["userId"] as ByteArray,
                    endpoint = it["endpoint"] as String,
                    key = it["publicKey"] as String,
                    auth = it["auth"] as String
                )
            }
        }
    }

    suspend fun getMarksForUser(username: ByteArray): List<MarkInfo> = connection.inTransaction {
        it.sendPreparedStatement(
            query = """
                select MarkInformation.*, Module.title, Module.professor, group_concat(Mark.mark) as `marks` from MarkInformation
                join Module on MarkInformation.moduleCode = Module.code and MarkInformation.semesterCode = Module.semesterCode
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
                    typeId = it["typeId"] as String,
                    week = it["week"] as String,
                    date = (it["date"] as LocalDateTime).toDateTime(),
                    marks = (it["marks"] as String).split(",")
                )
            }
        }
    }

    suspend fun insertModules(modules: List<Module>) = connection.inTransaction {
        if (modules.isNotEmpty()) {
            it.sendPreparedStatement(
                query = """
                insert into Module (code, semesterCode, title, professor) 
                values ${modules.joinToString(", ") { "(?, ?, ?, ?)" }}
                on duplicate key update title=values(title), professor=values(professor)
            """.trimIndent(),
                values = modules.map { listOf(it.code, it.semesterCode, it.title, it.professor )}.flatten()
            )
        }

    }

    suspend fun insertMarks(marks: List<MarkInfo>, user: ByteArray) = connection.inTransaction {
        if (marks.isNotEmpty()) {
            it.sendPreparedStatement(
                query = """
                insert into MarkInformation (id, moduleCode, semesterCode, userId, typeId, week, date) 
                values ${marks.joinToString(", ") { "(?, ?, ?, ?, ?, ?, ?)" }}
            """.trimIndent(),
                values = marks.map { listOf(it.id.getBytes(), it.moduleCode, it.semesterCode, user, it.typeId, it.week, it.date )}.flatten()
            )

            it.sendPreparedStatement(
                query = """
                insert into Mark (id, markInformationId, mark) 
                values ${marks.flatMap { it.marks }.joinToString(", ") { "(?, ?, ?)" }}
            """.trimIndent(),
                values = marks.map { markInfo ->  markInfo.marks.map { listOf(UUID.randomUUID().getBytes(), markInfo.id.getBytes(), it) }.flatten()}.flatten()
            )
        }
    }

    suspend fun updateMarks(marks: List<MarkInfo>, user: ByteArray) = connection.inTransaction {
        if (marks.isNotEmpty()) {
            it.sendPreparedStatement(
                query = """
                insert into MarkInformation (id, moduleCode, semesterCode, userId, typeId, week, date) 
                values ${marks.joinToString(", ") { "(?, ?, ?, ?, ?, ?, ?)" }}
                on duplicate key update moduleCode=values(moduleCode), semesterCode=values(semesterCode), userId=values(userId), typeId=values(typeId), week=values(week), date=values(date)
            """.trimIndent(),
                values = marks.map { listOf(it.id.getBytes(), it.moduleCode, it.semesterCode, user, it.typeId, it.week, it.date )}.flatten()
            )

            it.sendPreparedStatement(
                query = """
                delete from Mark where markInformationId in (${marks.joinToString(", ") { "?" }})
            """.trimIndent(),
                values = marks.map { it.id.getBytes() }
            )

            it.sendPreparedStatement(
                query = """
                insert into Mark (id, markInformationId, mark) 
                values ${marks.flatMap { it.marks }.joinToString(", ") { "(?, ?, ?)" }}
            """.trimIndent(),
                values = marks.map { markInfo ->  markInfo.marks.map { listOf(UUID.randomUUID().getBytes(), markInfo.id.getBytes(), it) }.flatten()}.flatten()
            )
        }
    }
}