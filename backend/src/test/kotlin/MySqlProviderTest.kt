package org.ktugrades.backend

import com.github.jasync.sql.db.SuspendingConnection
import com.github.jasync.sql.db.asSuspending
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.ktugrades.backend.services.MySqlQueryProvider
import org.ktugrades.common.SubscriptionPayload
import org.ktugrades.common.UnsubscriptionPayload
import java.lang.RuntimeException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val dbConnection by lazy {
    ConfigFactory.load().getConfig("database").let {
        MySQLConnectionBuilder.createConnectionPool(
            it.getString("url") +
                "?user=${it.getString("user")}" +
                "&password=${it.getString("password")}"
        ).asSuspending
    }
}

val mySqlQueryProvider by lazy {
    MySqlQueryProvider()
}

object Fixtures {
    val User1 = User(
        username = "userName".toFixedByteArray(16),
        password = "userPass".toFixedByteArray(16)
    )
    val UserSubscription1 = UserSubscription(
        id = "userSubscription".toFixedByteArray(16).getUUID(),
        userId = User1.username.getUUID(),
        endpoint = "endpoint",
        publicKey = "publicKey",
        auth = "auth"
    )
    val MarkInfo1 = MarkInfo(
        id = "markInfo1".toFixedByteArray(16).getUUID(),
        moduleCode = "MOD1",
        semesterCode = "SEM1",
        title = "Module1",
        professor = "Professor1",
        typeId = "LA",
        week = "2",
        date = DateTime(2020, 1, 10, 20, 0, 0),
        averageMarkForModule = null,
        marks = listOf("5", "8")
    )
}

fun testDatabaseWithRollback(block: suspend (it: SuspendingConnection) -> Unit) = runBlocking {
    try {
        dbConnection.inTransaction<Nothing> {
            block(it)
            throw RuntimeException()
        }
    } catch (e: RuntimeException) { }
}

class MySqlProviderTest {
    @Test
    fun `It should get user`() = testDatabaseWithRollback {
        val user = mySqlQueryProvider.getUser(username = Fixtures.User1.username, conn = it)
        assertEquals(expected = Fixtures.User1, actual = user)
    }

    @Test
    fun `It should upsert user`() = testDatabaseWithRollback {
        val editedPass = "editedPass".toFixedByteArray(16)
        mySqlQueryProvider.upsertUser(username = Fixtures.User1.username, password = editedPass, conn = it)
        val user = mySqlQueryProvider.getUser(username = Fixtures.User1.username, conn = it)
        assertTrue { editedPass.contentEquals(user.password) }
    }

    @Test
    fun `It should insert user subscription`() = testDatabaseWithRollback {
        val payload = SubscriptionPayload(
            username = Fixtures.User1.username,
            endpoint = "",
            key = "",
            auth = ""
        )
        mySqlQueryProvider.insertUserSubscription(payload, it)
        val userSubscriptions = mySqlQueryProvider.getUsersWithSubscriptions(it)
        assertTrue { userSubscriptions.find { it.user ==  Fixtures.User1}?.subscriptions?.size == 2 }
    }

    @Test
    fun `It should delete user subscription`() = testDatabaseWithRollback {
        val payload = UnsubscriptionPayload(
            endpoint = Fixtures.UserSubscription1.endpoint,
            key = Fixtures.UserSubscription1.publicKey,
            auth = Fixtures.UserSubscription1.auth
        )
        mySqlQueryProvider.deleteUserSubscription(payload, it)
        val userSubscriptions = mySqlQueryProvider.getUsersWithSubscriptions(it)
        assertTrue { userSubscriptions.find { it.user ==  Fixtures.User1} == null }
    }

    @Test
    fun `It should insert new mark information`() = testDatabaseWithRollback {
        val newMarks = listOf(
            MarkInfo(
                id = "markInfo2".toFixedByteArray(16).getUUID(),
                moduleCode = "MOD1",
                semesterCode = "SEM1",
                title = "Module1",
                professor = "Professor1",
                typeId = "LA",
                week = "16",
                date = DateTime(2020, 1, 1, 0, 0),
                averageMarkForModule = null,
                marks = listOf("10", "NE")
            ),
            MarkInfo(
                id = "markInfo3".toFixedByteArray(16).getUUID(),
                moduleCode = "MOD2",
                semesterCode = "SEM2",
                title = "Module2",
                professor = "Professor2",
                typeId = "LA",
                week = "7",
                date = DateTime(2020, 1, 1, 0, 0),
                averageMarkForModule = null,
                marks = listOf("6")
            )
        )

        mySqlQueryProvider.insertNewMarkInformation(marks = newMarks, user = Fixtures.User1.username, conn = it)
        val allMarks = mySqlQueryProvider.getMarksForUser(username = Fixtures.User1.username, conn = it)

        assertTrue { allMarks.size == 3 }
    }

    @Test
    fun `It should update mark information`() = testDatabaseWithRollback {
        val updatedMark = Fixtures.MarkInfo1.copy(
            date = DateTime(2020, 1, 11, 13, 30, 30),
            marks = listOf("6")
        )

        mySqlQueryProvider.updateMarks(marks = listOf(updatedMark), user = Fixtures.User1.username, conn = it)
        val allMarks = mySqlQueryProvider.getMarksForUser(username = Fixtures.User1.username, conn = it)
        assertEquals(expected = updatedMark, actual = allMarks.find { it.id == Fixtures.MarkInfo1.id })
    }

    @Test
    fun `It should get mark averages`() = testDatabaseWithRollback {
        val averages = mySqlQueryProvider.getAverageMarks(it)
        assertEquals(expected = 6.5, actual = averages.first().averageMark)
    }

    @Test
    fun `It should insert mark averages`() = testDatabaseWithRollback {
        val markSlotWithAverage = MarkSlot(
            moduleCode = Fixtures.MarkInfo1.moduleCode,
            semesterCode = Fixtures.MarkInfo1.semesterCode,
            typeId = Fixtures.MarkInfo1.typeId,
            week = Fixtures.MarkInfo1.week,
            averageMark = 8.4
        )

        mySqlQueryProvider.insertMarkSlotAverages(markSlots = listOf(markSlotWithAverage), conn = it)
        val allMarks = mySqlQueryProvider.getMarksForUser(username = Fixtures.User1.username, conn = it)
        assertEquals(expected = markSlotWithAverage.averageMark, actual = allMarks.find { it.id == Fixtures.MarkInfo1.id }?.averageMarkForModule)
    }
}