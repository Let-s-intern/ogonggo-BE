package com.ogonggo.userapi.notification.alert

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "ogonggo.notification.alerts")
data class NotificationAlertProperties(
    val enabled: Boolean = false,
    val environment: String = "unknown",
    val instanceId: String = "unknown",
    val cooldown: Duration = Duration.ofMinutes(15),
    val minSendInterval: Duration = Duration.ofSeconds(1),
    val checkInterval: Duration = Duration.ofSeconds(30),
    val stalledAfter: Duration = Duration.ofMinutes(2),
) {
    init {
        require(!cooldown.isNegative && !cooldown.isZero) { "경고 재전송 간격은 양수여야 합니다." }
        require(minSendInterval >= Duration.ofSeconds(1)) { "Slack 전송 간격은 1초 이상이어야 합니다." }
        require(checkInterval >= Duration.ofSeconds(10)) { "경고 감시 간격은 10초 이상이어야 합니다." }
        require(stalledAfter >= checkInterval) { "진행 중단 기준은 감시 간격 이상이어야 합니다." }
        require(environment.length in 1..64 && instanceId.length in 1..128)
    }
}
