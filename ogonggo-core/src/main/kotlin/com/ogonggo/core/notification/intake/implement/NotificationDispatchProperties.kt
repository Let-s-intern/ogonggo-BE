package com.ogonggo.core.notification.intake.implement

import com.ogonggo.core.notification.domain.NotificationChannel
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** 새 알림에 적용할 기본 발송 창이다. 이미 저장된 발송 기한은 변경하지 않는다. */
@ConfigurationProperties(prefix = "ogonggo.notification.dispatch")
data class NotificationDispatchProperties(
    val defaultWindow: Duration = Duration.ofMinutes(8),
    val windows: Map<NotificationChannel, Duration> = emptyMap(),
) {
    init {
        require(!defaultWindow.isZero && !defaultWindow.isNegative) { "기본 알림 발송 창은 양수여야 합니다." }
        require(windows.values.all { !it.isZero && !it.isNegative }) { "채널별 알림 발송 창은 양수여야 합니다." }
    }
}
