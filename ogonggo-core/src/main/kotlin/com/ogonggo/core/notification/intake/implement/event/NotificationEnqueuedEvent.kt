package com.ogonggo.core.notification.intake.implement.event

import com.ogonggo.core.notification.domain.NotificationChannel

/** 실제 적재 행 수를 트랜잭션 커밋 후 계측하기 위한 이벤트다. */
data class NotificationEnqueuedEvent(
    val channel: NotificationChannel,
    val count: Int,
)
