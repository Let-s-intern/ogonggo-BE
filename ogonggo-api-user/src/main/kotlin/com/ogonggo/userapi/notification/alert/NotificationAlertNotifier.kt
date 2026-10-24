package com.ogonggo.userapi.notification.alert

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component

/** 감지기에서 호출하는 진입점. Slack 지연·장애를 업무 트랜잭션이나 발송 실행기로 전파하지 않는다. */
@Component
class NotificationAlertNotifier internal constructor(
    private val sender: NotificationAlertSender,
    private val properties: NotificationAlertProperties,
    @Qualifier(NOTIFICATION_ALERT_TASK_EXECUTOR) private val executor: TaskExecutor,
    private val meterRegistry: MeterRegistry,
) {
    fun notify(alert: NotificationAlert) {
        if (!properties.enabled) return
        try {
            executor.execute { sender.send(alert) }
        } catch (_: TaskRejectedException) {
            meterRegistry.counter("ogonggo.notification.alerts", "type", alert.type.name, "outcome", "REJECTED").increment()
            log.error("알림 운영 경고 실행기가 포화되거나 종료되어 제출하지 못했습니다. type={}", alert.type.name)
        }
    }

    private companion object {
        val log = LoggerFactory.getLogger(NotificationAlertNotifier::class.java)
    }
}
