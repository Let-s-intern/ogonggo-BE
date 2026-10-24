package com.ogonggo.userapi.notification.delivery.implement

import com.ogonggo.core.notification.delivery.implement.NotificationManager
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.userapi.config.UserAsyncConfiguration
import com.ogonggo.userapi.config.NotificationDeliveryProperties
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
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CancellationException
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Future
import java.util.LinkedHashMap

/** Due 알림을 채널 sender로 전달하고 provider 결과를 기록한다. */
@Component
class NotificationDispatcher internal constructor(
    private val notificationManager: NotificationManager,
    senders: List<NotificationSender>,
    @Qualifier(UserAsyncConfiguration.NOTIFICATION_DELIVERY_TASK_EXECUTOR)
    private val taskExecutor: TaskExecutor,
    @Qualifier(UserAsyncConfiguration.NOTIFICATION_RESULT_DB_TASK_EXECUTOR)
    private val resultDbTaskExecutor: TaskExecutor,
    private val notificationMetrics: NotificationMetrics,
    private val clock: Clock,
    private val deliveryProperties: NotificationDeliveryProperties = NotificationDeliveryProperties(),
) {
    private val sendersByChannel = senders.associateBy(NotificationSender::channel)

    init {
        require(sendersByChannel.size == senders.size) { "채널별 알림 발송기는 하나만 등록할 수 있습니다." }
    }

    /** ShedLock으로 작업 실행을 직렬화하고 행은 DB attempt token으로 조건부 선점한다. */
    @SchedulerLock(name = SCHEDULER_NAME, lockAtMostFor = LOCK_AT_MOST_FOR)
    fun dispatch() {
        val runId = UUID.randomUUID().toString()
        val startedAt = System.nanoTime()
        var summary = NotificationDispatchSummaryDto()

        try {
            val recovered = notificationManager.recoverStaleProcessing(
                now = LocalDateTime.now(clock),
                limitPerProvider = deliveryProperties.maxConcurrency,
            )
            if (recovered > 0) log.warn("stale 알림 처리 복구. runId={}, recovered={}", runId, recovered)
            summary = dispatchWithRollingWindow(runId, startedAt)
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

    /** 결과 저장까지 끝난 작업의 슬롯만 비워 다음 due 알림으로 보충한다. */
    private fun dispatchWithRollingWindow(
        runId: String,
        startedAt: Long,
    ): NotificationDispatchSummaryDto {
        val maxConcurrency = deliveryProperties.maxConcurrency
        val completionQueue = ArrayBlockingQueue<Future<NotificationDispatchOutcome>>(maxConcurrency)
        val completions = ExecutorCompletionService(taskExecutor, completionQueue)
        val inFlight = LinkedHashMap<Future<NotificationDispatchOutcome>, NotificationMessageDto>()
        var summary = NotificationDispatchSummaryDto()
        var accepting = true
        var interrupted = false

        try {
            while (accepting || inFlight.isNotEmpty()) {
                if (accepting && !hasTimeRemaining(startedAt)) accepting = false

                if (accepting && inFlight.size < maxConcurrency) {
                    val now = LocalDateTime.now(clock)
                    val dueNotifications = notificationManager.claimDue(
                        now = now,
                        limit = maxConcurrency - inFlight.size,
                    )
                    val candidates = dueNotifications

                    for (index in candidates.indices) {
                        val notification = candidates[index]
                        try {
                            val future = completions.submit(Callable {
                                deliver(runId, notification)
                            })
                            inFlight[future] = notification
                        } catch (_: TaskRejectedException) {
                            // claimDue는 후보 전체를 먼저 선점하므로, 이번 후보 뒤에 아직 제출하지 않은 행도 함께 반환한다.
                            candidates.drop(index).forEach { unsent ->
                                notificationManager.releaseClaim(
                                    notificationId = unsent.notificationId,
                                    attemptId = checkNotNull(unsent.processingAttemptId),
                                    now = LocalDateTime.now(clock),
                                )
                            }
                            summary = summary.copy(executorRejected = true)
                            accepting = false
                            log.warn("알림 전용 실행기가 포화되어 남은 대기 건을 다음 실행으로 넘깁니다. runId={}", runId)
                            break
                        }
                    }
                }

                if (inFlight.isEmpty()) break

                val completed = try {
                    completions.take()
                } catch (_: InterruptedException) {
                    // 신규 발송을 멈추되 이미 provider에 보낸 작업은 결과 저장까지 drain한다.
                    interrupted = true
                    accepting = false
                    continue
                }
                val notification = inFlight.remove(completed)
                    ?: error("완료된 알림 작업이 in-flight 목록에 없습니다.")
                val outcome = completedOutcome(runId, notification, completed)
                summary = summary.include(outcome)
                if (outcome == NotificationDispatchOutcome.UNRESOLVED) accepting = false
            }

            return summary
        } finally {
            // due 조회 등 coordinator 단계가 실패해도 이미 시작한 provider 호출의 DB 기록을 기다린다.
            while (inFlight.isNotEmpty()) {
                val completed = try {
                    completions.take()
                } catch (_: InterruptedException) {
                    interrupted = true
                    continue
                }
                val notification = inFlight.remove(completed) ?: continue
                summary = summary.include(completedOutcome(runId, notification, completed))
            }
            if (interrupted) Thread.currentThread().interrupt()
        }
    }

    private fun completedOutcome(
        runId: String,
        notification: NotificationMessageDto,
        completed: Future<NotificationDispatchOutcome>,
    ): NotificationDispatchOutcome = try {
        completed.get()
    } catch (exception: ExecutionException) {
        val cause = exception.cause ?: exception
        log.error(
            "알림 발송 작업이 결과 저장 전에 종료됐습니다. runId={}, notificationId={}, userNotificationId={}, errorType={}",
            runId,
            notification.notificationId,
            notification.userNotificationId,
            cause::class.simpleName,
            safeStackTrace(cause),
        )
        notificationMetrics.recordUnresolved(notification.channel)
        NotificationDispatchOutcome.UNRESOLVED
    } catch (exception: CancellationException) {
        log.error(
            "알림 발송 작업이 취소되어 결과를 확인할 수 없습니다. runId={}, notificationId={}, userNotificationId={}",
            runId,
            notification.notificationId,
            notification.userNotificationId,
        )
        notificationMetrics.recordUnresolved(notification.channel)
        NotificationDispatchOutcome.UNRESOLVED
    } catch (exception: InterruptedException) {
        Thread.currentThread().interrupt()
        log.error(
            "알림 발송 결과 대기가 중단됐습니다. runId={}, notificationId={}, userNotificationId={}",
            runId,
            notification.notificationId,
            notification.userNotificationId,
        )
        notificationMetrics.recordUnresolved(notification.channel)
        NotificationDispatchOutcome.UNRESOLVED
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
            "알림 sender 예외. runId={}, notificationId={}, userNotificationId={}, errorType={}",
            runId,
            notification.notificationId,
            notification.userNotificationId,
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
            val recorded = CompletableFuture.supplyAsync(
                {
                    notificationManager.complete(
                        notificationId = notification.notificationId,
                        attemptId = checkNotNull(notification.processingAttemptId),
                        result = result,
                        now = LocalDateTime.now(clock),
                    )
                },
                resultDbTaskExecutor,
            ).join()
            if (!recorded) {
                log.error("알림 발송 결과를 기록하지 못했습니다. runId={}, notificationId={}, userNotificationId={}", runId, notification.notificationId, notification.userNotificationId)
                notificationMetrics.recordUnresolved(notification.channel)
                NotificationDispatchOutcome.UNRESOLVED
            } else {
                notificationMetrics.recordDelivery(notification.channel, result)
                logDeliveryResult(runId, notification, result)
                result.toDispatchOutcome()
            }
        } catch (exception: Exception) {
            log.error(
                "알림 발송 결과가 미확정입니다. runId={}, notificationId={}, userNotificationId={}, errorType={}",
                runId,
                notification.notificationId,
                notification.userNotificationId,
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
                "알림 provider 접수. runId={}, notificationId={}, userNotificationId={}, channel={}",
                runId,
                notification.notificationId,
                notification.userNotificationId,
                notification.channel,
            )
            is NotificationDeliveryResult.Failed -> log.warn(
                "알림 발송 실패. runId={}, notificationId={}, userNotificationId={}, channel={}, failureCategory={}, resultCode={}, resultDetail={}",
                runId,
                notification.notificationId,
                notification.userNotificationId,
                notification.channel,
                result.failureCategory,
                result.resultCode,
                result.detail,
            )
            is NotificationDeliveryResult.Unknown -> log.warn(
                "알림 provider 접수 여부 미확정. runId={}, notificationId={}, userNotificationId={}, channel={}, failureCategory={}, resultCode={}, resultDetail={}",
                runId,
                notification.notificationId,
                notification.userNotificationId,
                notification.channel,
                result.failureCategory,
                result.resultCode,
                result.detail,
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

    private fun safeStackTrace(exception: Throwable) = RuntimeException("알림 처리 실패").apply {
        // ORM/provider 예외 메시지나 cause에 포함될 수 있는 요청 데이터는 로그에 남기지 않는다.
        stackTrace = exception.stackTrace
    }

    companion object {
        /** 모든 채널의 알림을 발송하는 공용 DB 작업 키다. */
        const val SCHEDULER_NAME = "notificationDelivery"
        private const val NANOS_PER_MILLISECOND = 1_000_000
        private const val LONG_RUN_WARNING_MILLIS = 60_000L
        private const val LOCK_AT_MOST_FOR = "PT90S"
        private const val LOCK_AT_MOST_FOR_MILLIS = 90_000L
        private val RUN_BUDGET = Duration.ofSeconds(45)
        private val log = LoggerFactory.getLogger(NotificationDispatcher::class.java)

        internal fun shouldWarnLongRun(durationMillis: Long): Boolean = durationMillis >= LONG_RUN_WARNING_MILLIS
    }
}
