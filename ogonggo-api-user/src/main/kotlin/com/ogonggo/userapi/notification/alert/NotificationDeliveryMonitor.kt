package com.ogonggo.userapi.notification.alert

import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.schedule.implement.ScheduledJobReader
import com.ogonggo.userapi.notification.delivery.implement.NotificationDispatcher
import com.ogonggo.userapi.notification.metrics.NotificationDispatchWaitWindow
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** 업무 스케줄러와 분리해, 발송 대기와 최근 확정 처리 유무만 공용 DB에서 확인한다. */
@Component
class NotificationDeliveryMonitor(
    private val notificationManager: NotificationManager,
    private val notifier: NotificationAlertNotifier,
    private val properties: NotificationAlertProperties,
    private val clock: Clock,
    private val scheduledJobReader: ScheduledJobReader,
    private val waitWindow: NotificationDispatchWaitWindow,
) {
    private var executor: ScheduledExecutorService? = null
    private var pendingSince: LocalDateTime? = null
    private var laggingSince: LocalDateTime? = null

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        if (!properties.enabled || executor != null) return
        executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon(true).name("notification-monitor-", 0).factory(),
        ).also {
            it.scheduleWithFixedDelay(::check, properties.checkInterval.toMillis(), properties.checkInterval.toMillis(), TimeUnit.MILLISECONDS)
        }
    }

    internal fun check() {
        if (!properties.enabled) return
        try {
            if (scheduledJobReader.read(NotificationDispatcher.SCHEDULER_NAME)?.enabled == false) {
                pendingSince = null
                laggingSince = null
                return
            }
            val now = LocalDateTime.now(clock)
            checkLagging(now)
            checkAdminBatchSlow(now)
            val due = notificationManager.countPendingDue(now)
            val staleProcessing = notificationManager.countStaleProcessing(now)
            if (due == 0L && staleProcessing == 0L) {
                pendingSince = null
                return
            }
            if (staleProcessing > 0L) {
                pendingSince = null
                notifier.notify(
                    NotificationAlertType.DELIVERY_STALLED,
                    measurements = mapOf("staleProcessing" to staleProcessing.toString()),
                )
                return
            }
            val since = pendingSince ?: now.also { pendingSince = it }
            // 새 알림 도착 직후 또는 재기동 직후의 조용한 구간을 장애로 오인하지 않는다.
            if (now.isBefore(since.plus(properties.stalledAfter))) return
            if (!notificationManager.hasDeliveryProgressSince(now.minus(properties.stalledAfter))) {
                notifier.notify(
                    NotificationAlertType.DELIVERY_STALLED,
                    measurements = mapOf("due" to due.toString(), "noProgressSeconds" to properties.stalledAfter.seconds.toString()),
                )
            }
        } catch (exception: Exception) {
            // DB 예외 원문에 SQL·수신자 정보가 포함될 수 있으므로 분류만 남긴다.
            log.warn("알림 진행 감시에 실패했습니다. errorType={}", exception::class.simpleName)
            notifier.notify(NotificationAlertType.MONITORING_FAILURE)
        }
    }

    /**
     * 최근 구간에 꺼낸 개별 알림의 대기 p95가 기준 이상인 상태가 일정 시간 이어지면 경보를 보낸다.
     * 이 인스턴스가 발송 작업을 돌리지 않았다면 표본이 없어 판단하지 않는다.
     */
    private fun checkLagging(now: LocalDateTime) {
        val percentile = waitWindow.p95Since(clock.instant().minus(properties.lagWindow))
        if (percentile == null || percentile.p95 < properties.lagThreshold) {
            laggingSince = null
            return
        }
        val since = laggingSince ?: now.also { laggingSince = it }
        if (now.isBefore(since.plus(properties.lagSustainedFor))) return
        notifier.notify(
            NotificationAlertType.DELIVERY_LAGGING,
            measurements = mapOf(
                "p95Millis" to percentile.p95.toMillis().toString(),
                "samples" to percentile.samples.toString(),
                "laggingSeconds" to java.time.Duration.between(since, now).seconds.toString(),
            ),
        )
    }

    /** 관리자 발송 알림이 예정 시각부터 기준 시간 넘게 대기 중이면 경보를 보낸다. 공용 DB로 판단하므로 인스턴스와 무관하다. */
    private fun checkAdminBatchSlow(now: LocalDateTime) {
        if (!notificationManager.hasAdminBatchWaitingLongerThan(now, properties.adminBatchSlowAfter)) return
        notifier.notify(
            NotificationAlertType.ADMIN_BATCH_SLOW,
            measurements = mapOf("waitingOverMinutes" to properties.adminBatchSlowAfter.toMinutes().toString()),
        )
    }

    @PreDestroy
    fun close() {
        executor?.shutdownNow()
    }

    private companion object {
        val log = LoggerFactory.getLogger(NotificationDeliveryMonitor::class.java)
    }
}
