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
    /** 개별 알림 대기 p95가 이 값 이상이면 지연으로 본다. */
    val lagThreshold: Duration = Duration.ofSeconds(3),
    /** p95를 계산하는 최근 구간 길이. */
    val lagWindow: Duration = Duration.ofMinutes(1),
    /** 지연 상태가 이만큼 이어지면 경보를 보낸다. */
    val lagSustainedFor: Duration = Duration.ofMinutes(2),
    /** 관리자 발송 알림이 예정 시각부터 이만큼 넘게 대기하면 경보를 보낸다. */
    val adminBatchSlowAfter: Duration = Duration.ofMinutes(10),
) {
    init {
        require(!lagThreshold.isNegative && !lagThreshold.isZero) { "발송 지연 기준은 양수여야 합니다." }
        require(!lagWindow.isNegative && !lagWindow.isZero) { "발송 지연 계산 구간은 양수여야 합니다." }
        require(lagSustainedFor >= checkInterval) { "발송 지연 지속 기준은 감시 간격 이상이어야 합니다." }
        require(adminBatchSlowAfter >= checkInterval) { "관리자 발송 지연 기준은 감시 간격 이상이어야 합니다." }
        require(!cooldown.isNegative && !cooldown.isZero) { "경고 재전송 간격은 양수여야 합니다." }
        require(minSendInterval >= Duration.ofSeconds(1)) { "Slack 전송 간격은 1초 이상이어야 합니다." }
        require(checkInterval >= Duration.ofSeconds(10)) { "경고 감시 간격은 10초 이상이어야 합니다." }
        require(stalledAfter >= checkInterval) { "진행 중단 기준은 감시 간격 이상이어야 합니다." }
        require(environment.length in 1..64 && instanceId.length in 1..128)
    }
}
