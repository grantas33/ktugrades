package org.ktugrades.backend

import com.typesafe.config.ConfigFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.ktugrades.backend.handlers.DataHandler
import org.ktugrades.backend.handlers.LoginHandler
import org.ktugrades.backend.services.MarkService
import org.ktugrades.backend.services.NotificationService
import org.ktugrades.common.Credentials
import kotlin.test.assertEquals

class NotificationServiceTest {

    private val testUser by lazy {
        ConfigFactory.load().getConfig("testUser").let {
            Credentials(it.getString("username"), it.getString("password"))
        }
    }

    private val encryptedTestUser by lazy {
        User(username = ByteArray(16), password = ByteArray(16))
    }

    private val notificationService by lazy {
        NotificationService(
            mySqlProvider = mockk(relaxed = true),
            credentialProvider = mockk { coEvery { getCredentials(any(), any()) } returns testUser },
            loginHandler = LoginHandler(),
            markService = MarkService(
                dataHandler = DataHandler(),
                mySqlProvider = mockk { coEvery { getMarksForUser(any()) } returns emptyList() }
            ),
            jsonSerializer = mockk(relaxed = true)
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `It should be able to receive grade data from AIS in repeating successions`() = runBlocking {
        val testCount = 10
        var successCount = 0;
        repeat(testCount) {
            withCoroutineClient {
                val isSuccess = try {
                    notificationService.sendNotificationToUserSubscriptions(
                        data = UserSubscriptionData(user = encryptedTestUser, subscriptions = emptyList()),
                        pushService = mockk(relaxed = true)
                    )
                    true
                } catch (e: Exception) { false }
                if (isSuccess) successCount++
                println("Test ${it+1}: ${if (isSuccess) "success" else "fail"}")
            }
        }
        assertEquals(expected = testCount, actual = successCount)
    }
}
