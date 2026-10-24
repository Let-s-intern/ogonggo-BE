package com.ogonggo.core.notification.intake.implement

import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
@EnableConfigurationProperties(NotificationDispatchProperties::class)
class NotificationDispatchPolicy(private val properties: NotificationDispatchProperties) {
    /** 명시한 기한을 우선하고, 없으면 예정 시각과 적재 시각 중 늦은 시각부터 채널별 창을 적용한다. */
    fun expiresAt(notification: NotificationAppendDto, enqueuedAt: LocalDateTime): LocalDateTime =
        notification.dispatchExpiresAt ?: maxOf(notification.scheduledAt, enqueuedAt).plus(
            properties.windows[notification.channel] ?: properties.defaultWindow,
        )
}
