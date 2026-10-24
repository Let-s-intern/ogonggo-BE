package com.ogonggo.core.notification.intake.implement

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.core.notification.intake.implement.event.NotificationEnqueuedEvent
import com.ogonggo.core.notification.persistence.NotificationJpaRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationAppenderEventTest {
    @Test
    fun `새 행을 적재하면 채널별 삽입 수를 이벤트로 발행한다`() {
        val repository = Mockito.mock(NotificationJpaRepository::class.java)
        val eventPublisher = Mockito.mock(ApplicationEventPublisher::class.java)
        Mockito.`when`(repository.findAllByDeduplicationKeyIn(listOf("key"))).thenReturn(emptyList())
        val appender = NotificationAppender(
            repository,
            eventPublisher,
            NotificationDispatchPolicy(NotificationDispatchProperties()),
            Clock.fixed(Instant.parse("2026-10-07T03:00:00Z"), ZoneId.of("Asia/Seoul")),
        )

        appender.append(
            NotificationAppendDto(
                deduplicationKey = "key",
                channel = NotificationChannel.KAKAO,
                templateCode = "sign_up_confirm",
                recipientAddress = "01012345678",
                payloadJson = "{}",
                scheduledAt = LocalDateTime.of(2026, 10, 7, 12, 0),
            ),
        )

        Mockito.verify(eventPublisher).publishEvent(NotificationEnqueuedEvent(NotificationChannel.KAKAO, 1))
    }
}
