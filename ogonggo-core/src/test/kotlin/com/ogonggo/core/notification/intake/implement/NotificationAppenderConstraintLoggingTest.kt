package com.ogonggo.core.notification.intake.implement

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.ogonggo.core.notification.domain.Notification
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException
import java.time.LocalDateTime

internal class NotificationAppenderConstraintLoggingTest {

    @Test
    @DisplayName("동시 deduplication_key 삽입 경합을 개인정보 없이 전용 로그로 남기고 예외를 전파한다")
    fun `deduplication key unique 경합 시 전용 로그를 남기고 예외를 전파한다`() {
        // given
        val repository = Mockito.mock(NotificationJpaRepository::class.java)
        val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
        val appender = NotificationAppender(repository, eventPublisher)
        val sensitiveKey = "signup-confirm:user:42:KAKAO"
        val databaseException = SQLException(
            "Duplicate entry '$sensitiveKey' for key 'notifications.uk_notification_deduplication_key'",
            "23000",
            1062,
        )
        val persistenceException = DataIntegrityViolationException("Could not persist notification batch", databaseException)
        Mockito.`when`(
            repository.findAllByDeduplicationKeyIn(ArgumentMatchers.anyList<String>()),
        ).thenReturn(emptyList())
        Mockito.`when`(
            repository.saveAllAndFlush(ArgumentMatchers.anyList<Notification>()),
        ).thenThrow(persistenceException)

        val logger = LoggerFactory.getLogger(NotificationAppender::class.java) as Logger
        val logAppender = ListAppender<ILoggingEvent>().apply {
            context = logger.loggerContext
            start()
        }
        logger.addAppender(logAppender)

        try {
            // when
            val thrown = assertThrows<DataIntegrityViolationException> {
                appender.appendAll(listOf(notification("candidate-a"), notification("candidate-b")))
            }

            // then
            assertSame(persistenceException, thrown)
            val logMessage = logAppender.list.single().formattedMessage
            assertTrue(logMessage.contains("deduplication_key"))
            assertTrue(logMessage.contains("batchSize=2"))
            assertFalse(logMessage.contains(sensitiveKey))
            Mockito.verifyNoInteractions(eventPublisher)
        } finally {
            logger.detachAppender(logAppender)
            logAppender.stop()
        }
    }

    private fun notification(deduplicationKey: String) = NotificationAppendDto(
        deduplicationKey = deduplicationKey,
        channel = NotificationChannel.KAKAO,
        templateCode = "sign_up_confirm",
        recipientAddress = "01012345678",
        payloadJson = "{}",
        scheduledAt = LocalDateTime.of(2026, 10, 7, 12, 0),
    )
}
