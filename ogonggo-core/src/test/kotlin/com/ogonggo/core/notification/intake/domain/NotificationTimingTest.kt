package com.ogonggo.core.notification.intake.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NotificationTimingTest {

    @Test
    @DisplayName("기준 시각보다 24시간 이른 시각부터 알림이 발송 대상이다")
    fun `24시간 전 정책은 기준 시각에서 하루를 뺀다`() {
        // given
        val referenceAt = LocalDateTime.of(2026, 10, 10, 12, 30)

        // when
        val scheduledAt = NotificationTiming.MINUS_24_HOURS.calculateAt(referenceAt)

        // then
        assertEquals(LocalDateTime.of(2026, 10, 9, 12, 30), scheduledAt)
    }
}
