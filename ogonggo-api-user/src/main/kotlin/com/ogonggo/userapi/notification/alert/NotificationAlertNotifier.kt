package com.ogonggo.userapi.notification.alert

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/** 감지기에서 호출하는 진입점. Slack 지연·장애를 업무 트랜잭션이나 발송 실행기로 전파하지 않는다. */
@Component
class NotificationAlertNotifier internal constructor(
    private val sender: NotificationAlertSender,
    private val properties: NotificationAlertProperties,
    @Qualifier(NOTIFICATION_ALERT_TASK_EXECUTOR) private val executor: TaskExecutor,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    // scope나 알림 ID를 키로 보관하지 않는다. 현재 8유형 × 3상태 = 24자리만 사용한다.
    private val nextAllowedAt = arrayOfNulls<Instant>(NotificationAlertType.entries.size * NotificationAlertStatus.entries.size)

    fun notify(type: NotificationAlertType, scope: String = "all", measurements: Map<String, String> = emptyMap()) {
        if (!properties.enabled) return
        notify(NotificationAlert(type, LocalDateTime.now(clock), scope = scope, measurements = measurements))
    }

    fun notify(alert: NotificationAlert) {
        if (!properties.enabled) return
        if (!reserve(alert)) {
            meterRegistry.counter("ogonggo.notification.alerts", "type", alert.type.name, "outcome", "SUPPRESSED").increment()
            return
        }
        try {
            executor.execute { sender.send(alert) }
        } catch (_: TaskRejectedException) {
            meterRegistry.counter("ogonggo.notification.alerts", "type", alert.type.name, "outcome", "REJECTED").increment()
            log.error("알림 운영 경고 실행기가 포화되거나 종료되어 제출하지 못했습니다. type={}", alert.type.name)
        }
    }

    /** 큐 제출 전에 예약한다. 제출 거절·HTTP 실패에도 취소하지 않아 장애 중 재시도 폭주를 막는다. */
    @Synchronized
    private fun reserve(alert: NotificationAlert): Boolean {
        val index = alert.type.ordinal * NotificationAlertStatus.entries.size + alert.status.ordinal
        val now = clock.instant()
        if (nextAllowedAt[index]?.isAfter(now) == true) return false
        nextAllowedAt[index] = now.plus(properties.cooldown)
        return true
    }

    private companion object {
        val log = LoggerFactory.getLogger(NotificationAlertNotifier::class.java)
    }
}
