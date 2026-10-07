package com.ogonggo.userapi.notification.metrics

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.intake.implement.event.NotificationEnqueuedEvent
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/** 알림 적재량·발송 결과·DB 대기 잔량을 Actuator MeterRegistry에 기록한다. */
@Component
class NotificationMetrics(
    private val meterRegistry: MeterRegistry,
    private val notificationManager: NotificationManager,
    private val clock: Clock,
) {
    init {
        PendingState.values().forEach(::registerPendingGauge)
    }

    /** 적재 이벤트는 Appender 트랜잭션이 실제 커밋된 뒤에만 누적한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onNotificationEnqueued(event: NotificationEnqueuedEvent) {
        if (event.count <= 0) return
        meterRegistry.counter(
            ENQUEUED_METRIC,
            CHANNEL_TAG,
            event.channel.name,
        ).increment(event.count.toDouble())
    }

    fun recordDelivery(channel: NotificationChannel, result: NotificationDeliveryResult) {
        val (outcome, failureCategory) = when (result) {
            is NotificationDeliveryResult.Sent -> SENT to NO_FAILURE_CATEGORY
            is NotificationDeliveryResult.Failed -> FAILED to result.failureCategory.name
            is NotificationDeliveryResult.Unknown -> UNKNOWN to result.failureCategory.name
        }
        recordDelivery(channel, outcome, failureCategory)
    }

    fun recordUnresolved(channel: NotificationChannel) {
        recordDelivery(channel, UNRESOLVED, NO_FAILURE_CATEGORY)
    }

    private fun registerPendingGauge(state: PendingState) {
        Gauge.builder(PENDING_METRIC, this) { metrics -> metrics.readPendingCount(state).toDouble() }
            .tag(STATE_TAG, state.tag)
            .description("notifications의 PENDING 건수. due/expired는 dispatcher 8분 발송 창 기준")
            .register(meterRegistry)
    }

    private fun readPendingCount(state: PendingState): Long {
        val now = LocalDateTime.now(clock)
        val notBefore = now.minus(PENDING_DISPATCH_WINDOW)
        return when (state) {
            PendingState.TOTAL -> notificationManager.countPendingTotal()
            PendingState.DUE -> notificationManager.countPendingDue(now, notBefore)
            PendingState.EXPIRED -> notificationManager.countPendingExpired(notBefore)
            PendingState.FUTURE -> notificationManager.countPendingFuture(now)
        }
    }

    private fun recordDelivery(channel: NotificationChannel, outcome: String, failureCategory: String) {
        meterRegistry.counter(
            DELIVERY_METRIC,
            CHANNEL_TAG,
            channel.name,
            OUTCOME_TAG,
            outcome,
            FAILURE_CATEGORY_TAG,
            failureCategory,
        ).increment()
    }

    private enum class PendingState(val tag: String) {
        TOTAL("total"),
        DUE("due"),
        EXPIRED("expired"),
        FUTURE("future"),
    }

    private companion object {
        const val ENQUEUED_METRIC = "ogonggo.notification.enqueued"
        const val DELIVERY_METRIC = "ogonggo.notification.delivery"
        const val PENDING_METRIC = "ogonggo.notification.pending"
        const val CHANNEL_TAG = "channel"
        const val OUTCOME_TAG = "outcome"
        const val FAILURE_CATEGORY_TAG = "failure_category"
        const val STATE_TAG = "state"
        const val NO_FAILURE_CATEGORY = "NONE"
        const val SENT = "SENT"
        const val FAILED = "FAILED"
        const val UNKNOWN = "UNKNOWN"
        const val UNRESOLVED = "UNRESOLVED"
        val PENDING_DISPATCH_WINDOW: Duration = Duration.ofMinutes(8)
    }
}
