package com.ogonggo.userapi.notification.fcm.business

import com.google.api.core.ApiFutures
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.TopicManagementResponse
import com.ogonggo.core.fcmtopicsynctask.implement.FcmTopicSyncTaskManager
import com.ogonggo.core.fcmtopicsynctask.implement.dto.FcmTopicSyncTaskDto
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.ObjectProvider
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.*

internal class FcmTopicSyncServiceTest {
    private val tasks = Mockito.mock(FcmTopicSyncTaskManager::class.java)
    private val firebase = Mockito.mock(FirebaseMessaging::class.java)
    @Suppress("UNCHECKED_CAST")
    private val provider = Mockito.mock(ObjectProvider::class.java) as ObjectProvider<FirebaseMessaging>
    private val transactionManager = Mockito.mock(PlatformTransactionManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-24T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val now = LocalDateTime.now(clock)
    private val first = FcmTopicSyncTaskDto(1, 0, TOPIC, "first", true, 0)
    private val second = FcmTopicSyncTaskDto(2, 0, TOPIC, "second", true, 0)
    private val service = FcmTopicSyncService(tasks, NotificationBroadcastProperties(topic = TOPIC), provider,
        TransactionTemplate(transactionManager), clock)

    @BeforeEach
    fun setup() {
        Mockito.`when`(provider.ifAvailable).thenReturn(firebase)
        Mockito.`when`(transactionManager.getTransaction(Mockito.any())).thenReturn(SimpleTransactionStatus())
        Mockito.`when`(tasks.pending(now, 1000)).thenReturn(listOf(first, second), emptyList())
    }

    @Test
    @DisplayName("일괄 구독의 일부 실패는 해당 토큰만 재처리하고 나머지는 완료한다")
    fun `부분 실패를 분리한다`() {
        // given
        val response = Mockito.mock(TopicManagementResponse::class.java)
        val error = Mockito.mock(TopicManagementResponse.Error::class.java)
        Mockito.`when`(error.index).thenReturn(1)
        Mockito.`when`(error.reason).thenReturn("internal-error")
        Mockito.`when`(response.errors).thenReturn(listOf(error))
        Mockito.`when`(firebase.subscribeToTopicAsync(listOf("first", "second"), TOPIC)).thenReturn(ApiFutures.immediateFuture(response))
        // when
        service.sync()
        // then
        Mockito.verify(tasks).finish(first, now)
        Mockito.verify(tasks).retry(second, "internal-error", now)
        Mockito.verify(tasks, Mockito.never()).finish(second, now)
    }

    @Test
    @DisplayName("구독 호출 전체 실패는 토큰 원문이 섞인 외부 메시지 대신 안전한 코드로 재처리한다")
    fun `전체 실패는 두 작업을 재처리한다`() {
        // given
        Mockito.`when`(firebase.subscribeToTopicAsync(listOf("first", "second"), TOPIC))
            .thenReturn(ApiFutures.immediateFailedFuture(IllegalStateException("secret-token-value")))
        // when
        service.sync()
        // then
        Mockito.verify(tasks).retry(first, "FCM_SUBSCRIPTION_ERROR", now)
        Mockito.verify(tasks).retry(second, "FCM_SUBSCRIPTION_ERROR", now)
        Mockito.verify(tasks, Mockito.never()).finish(first, now)
    }

    companion object { private const val TOPIC = "ogonggo-all-test" }
}
