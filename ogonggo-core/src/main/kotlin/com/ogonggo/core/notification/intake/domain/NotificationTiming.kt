package com.ogonggo.core.notification.intake.domain

import java.time.LocalDateTime

/** 기준 시각을 바탕으로 알림이 발송 worker의 처리 대상이 되는 시각을 계산한다. */
enum class NotificationTiming(private val offsetHours: Long) {
    MINUS_24_HOURS(-24);

    fun calculateAt(referenceAt: LocalDateTime): LocalDateTime = referenceAt.plusHours(offsetHours)
}
