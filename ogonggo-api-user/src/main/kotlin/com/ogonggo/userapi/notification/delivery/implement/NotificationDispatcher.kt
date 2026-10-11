package com.ogonggo.userapi.notification.delivery.implement

import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.userapi.config.UserAsyncConfiguration
import com.ogonggo.userapi.notification.metrics.NotificationMetrics
import com.ogonggo.userapi.notification.delivery.implement.dto.NotificationDispatchOutcome
import com.ogonggo.userapi.notification.delivery.implement.dto.NotificationDispatchSummaryDto
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture

/** Due 알림을 채널 sender로 전달하고 provider 결과를 기록한다. */
@Component
class NotificationDispatcher internal constructor(
    private val notificationManager: NotificationManager,
    senders: List<NotificationSender>,
    @Qualifier(UserAsyncConfiguration.NOTIFICATION_DELIVERY_TASK_EXECUTOR)
    private val taskExecutor: TaskExecutor,
    private val notificationMetrics: NotificationMetrics,
    private val clock: Clock,
) {
    private val sendersByChannel = senders.associateBy(NotificationSender::channel)

    init {
        require(sendersByChannel.size == senders.size) { "채널별 알림 발송기는 하나만 등록할 수 있습니다." }
    }

    /** 행별 claim 대신 한 번의 발송 작업을 ShedLock으로 직렬화한다. */
    @SchedulerLock(name = SCHEDULER_NAME, lockAtMostFor = LOCK_AT_MOST_FOR)
    fun dispatch() {
        val runId = UUID.randomUUID().toString()
        val startedAt = System.nanoTime()
        var summary = NotificationDispatchSummaryDto()

        try {
            while (hasTimeRemaining(startedAt)) {
                val now = LocalDateTime.now(clock)
                val dueNotifications = notificationManager.findDue(
                    now = now,
                    notBefore = now.minus(PENDING_DISPATCH_WINDOW),
                    limit = BATCH_SIZE,
                )
                if (dueNotifications.isEmpty()) break

                val batchResult = dispatchBatch(runId, dueNotifications)
                summary += batchResult

                // 결과 기록 실패 건은 PENDING으로 남지만, 같은 실행에서는 재발송하지 않는다.
                if (batchResult.shouldStopNextBatch) break
            }
        } finally {
            val durationMs = elapsedMillis(startedAt)
            if (shouldWarnLongRun(durationMs)) {
                log.warn(
                    "알림 dispatcher 실행이 60초 이상 걸렸습니다. ShedLock 만료 전 실행 지연 신호입니다. runId={}, durationMs={}, lockAtMostForMs={}",
                    runId,
                    durationMs,
                    LOCK_AT_MOST_FOR_MILLIS,
                )
            }
            logRunSummary(runId, startedAt, summary)
        }
    }

    private fun dispatchBatch(
        runId: String,
        notifications: List<NotificationMessageDto>,
    ): NotificationDispatchSummaryDto {
        val deliveries = mutableListOf<CompletableFuture<NotificationDispatchOutcome>>()
        var executorRejected = false

        for (notification in notifications) {
            try {
                deliveries += CompletableFuture.supplyAsync(
                    { deliver(runId, notification) },
                    taskExecutor,
                )
            } catch (_: TaskRejectedException) {
                executorRejected = true
                log.warn("알림 전용 실행기가 포화되어 남은 대기 건을 다음 실행으로 넘깁니다. runId={}", runId)
                break
            }
        }

        val batchResult = deliveries.fold(NotificationDispatchSummaryDto()) { result, delivery ->
            result.include(delivery.join())
        }
        return batchResult.copy(executorRejected = executorRejected)
    }

    private fun deliver(
        runId: String,
        notification: NotificationMessageDto,
    ): NotificationDispatchOutcome {
        val result = sendToChannel(runId, notification)
        return recordResult(runId, notification, result)
    }

    private fun sendToChannel(
        runId: String,
        notification: NotificationMessageDto,
    ): NotificationDeliveryResult = try {
        sendersByChannel[notification.channel]?.send(notification)
            ?: NotificationDeliveryResult.Failed(
                resultCode = "CHANNEL_NOT_CONFIGURED",
                failureCategory = NotificationFailureCategory.CHANNEL_NOT_CONFIGURED,
            )
    } catch (exception: Exception) {
        log.error(
            "알림 sender 예외. runId={}, notificationId={}, errorType={}",
            runId,
            notification.notificationId,
            exception::class.simpleName,
            safeStackTrace(exception),
        )
        NotificationDeliveryResult.Unknown(
            resultCode = "UNCLASSIFIED_SENDER_ERROR",
            failureCategory = NotificationFailureCategory.APPLICATION_ERROR,
        )
    }

    private fun recordResult(
        runId: String,
        notification: NotificationMessageDto,
        result: NotificationDeliveryResult,
    ): NotificationDispatchOutcome {
        return try {
            val recorded = notificationManager.complete(
                notificationId = notification.notificationId,
                result = result,
                now = LocalDateTime.now(clock),
            )
            if (!recorded) {
                log.error("알림 발송 결과를 기록하지 못했습니다. runId={}, notificationId={}", runId, notification.notificationId)
                notificationMetrics.recordUnresolved(notification.channel)
                NotificationDispatchOutcome.UNRESOLVED
            } else {
                notificationMetrics.recordDelivery(notification.channel, result)
                logDeliveryResult(runId, notification, result)
                result.toDispatchOutcome()
            }
        } catch (exception: Exception) {
            log.error(
                "알림 발송 결과가 미확정입니다. runId={}, notificationId={}, errorType={}",
                runId,
                notification.notificationId,
                exception::class.simpleName,
                safeStackTrace(exception),
            )
            notificationMetrics.recordUnresolved(notification.channel)
            NotificationDispatchOutcome.UNRESOLVED
        }
    }

    private fun logDeliveryResult(
        runId: String,
        notification: NotificationMessageDto,
        result: NotificationDeliveryResult,
    ) {
        when (result) {
            is NotificationDeliveryResult.Sent -> log.debug(
                "알림 provider 접수. runId={}, notificationId={}, channel={}",
                runId,
                notification.notificationId,
                notification.channel,
            )
            is NotificationDeliveryResult.Failed -> log.warn(
                "알림 발송 실패. runId={}, notificationId={}, channel={}, failureCategory={}, resultCode={}",
                runId,
                notification.notificationId,
                notification.channel,
                result.failureCategory,
                result.resultCode,
            )
            is NotificationDeliveryResult.Unknown -> log.warn(
                "알림 provider 접수 여부 미확정. runId={}, notificationId={}, channel={}, failureCategory={}, resultCode={}",
                runId,
                notification.notificationId,
                notification.channel,
                result.failureCategory,
                result.resultCode,
            )
        }
    }

    private fun logRunSummary(
        runId: String,
        startedAt: Long,
        summary: NotificationDispatchSummaryDto,
    ) {
        if (!summary.hasResults) return

        log.info(
            "알림 발송 실행 완료. runId={}, sent={}, failed={}, unknown={}, unresolved={}, executorRejected={}, durationMs={}",
            runId,
            summary.sentCount,
            summary.failedCount,
            summary.unknownCount,
            summary.unresolvedCount,
            summary.executorRejected,
            elapsedMillis(startedAt),
        )
    }

    private fun hasTimeRemaining(startedAt: Long) =
        System.nanoTime() - startedAt < RUN_BUDGET.toNanos()

    private fun elapsedMillis(startedAt: Long) =
        (System.nanoTime() - startedAt) / NANOS_PER_MILLISECOND

    private fun NotificationDeliveryResult.toDispatchOutcome() = when (this) {
        is NotificationDeliveryResult.Sent -> NotificationDispatchOutcome.SENT
        is NotificationDeliveryResult.Failed -> NotificationDispatchOutcome.FAILED
        is NotificationDeliveryResult.Unknown -> NotificationDispatchOutcome.UNKNOWN
    }

    private fun safeStackTrace(exception: Exception) = RuntimeException("알림 처리 실패").apply {
        // ORM/provider 예외 메시지나 cause에 포함될 수 있는 요청 데이터는 로그에 남기지 않는다.
        stackTrace = exception.stackTrace
    }

    companion object {
        /** 모든 채널의 알림을 발송하는 공용 DB 작업 키다. */
        const val SCHEDULER_NAME = "notificationDelivery"
        private const val BATCH_SIZE = 4
        private const val NANOS_PER_MILLISECOND = 1_000_000
        private const val LONG_RUN_WARNING_MILLIS = 60_000L
        private const val LOCK_AT_MOST_FOR = "PT90S"
        private const val LOCK_AT_MOST_FOR_MILLIS = 90_000L
        private val PENDING_DISPATCH_WINDOW = Duration.ofMinutes(8)
        private val RUN_BUDGET = Duration.ofSeconds(45)
        private val log = LoggerFactory.getLogger(NotificationDispatcher::class.java)

        internal fun shouldWarnLongRun(durationMillis: Long): Boolean = durationMillis >= LONG_RUN_WARNING_MILLIS
    }
}
