package com.ogonggo.userapi.notification.fcm.business

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.ogonggo.core.fcmtopicsynctask.implement.FcmTopicSyncTaskManager
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.LocalDateTime
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class FcmTopicSyncService(
    private val tasks: FcmTopicSyncTaskManager,
    private val properties: NotificationBroadcastProperties,
    private val firebase: ObjectProvider<FirebaseMessaging>,
    private val transactions: TransactionTemplate,
    private val clock: Clock,
) {
    /** 외부 호출은 DB 트랜잭션 밖에서 수행하고 결과는 요청 revision이 유지될 때만 반영한다. */
    fun sync() {
        if (properties.syncTopics().isEmpty()) return
        val messaging = firebase.ifAvailable ?: return
        val started = System.nanoTime()
        var completed = 0
        var failed = 0
        var reconciled = 0
        do {
            val now = LocalDateTime.now(clock)
            // 후보를 작은 묶음마다 별도 트랜잭션으로 고친다. 한 트랜잭션에 1,000개를 넣으면 저장할 때마다
            // 그동안 읽은 엔티티 전체를 다시 검사해 처리 시간이 후보 수의 제곱으로 늘어난다.
            tasks.reconciliationCandidates(RECONCILE_LIMIT).chunked(RECONCILE_CHUNK).forEach { chunk ->
                reconciled += transactions.execute { tasks.reconcile(chunk, now) } ?: 0
            }
            val pending = tasks.pending(now, 1000)
            if (pending.isEmpty()) break
            for ((key, group) in pending.groupBy { it.topic to it.subscribed }) {
                val (topic, subscribed) = key
                val errors: Map<Int, String> = try {
                    val future = if (subscribed) messaging.subscribeToTopicAsync(group.map { it.token }, topic)
                        else messaging.unsubscribeFromTopicAsync(group.map { it.token }, topic)
                    val response = future.get(20, TimeUnit.SECONDS)
                    response.errors.associate { it.index to it.reason }
                } catch (exception: Exception) {
                    val cause = if (exception is ExecutionException) exception.cause else exception
                    if (cause is InterruptedException) Thread.currentThread().interrupt()
                    val code = when (cause) {
                        is FirebaseMessagingException -> cause.messagingErrorCode?.name ?: "FCM_SUBSCRIPTION_ERROR"
                        is TimeoutException -> "FCM_SUBSCRIPTION_TIMEOUT"
                        else -> "FCM_SUBSCRIPTION_ERROR"
                    }
                    group.indices.associateWith { code }
                }
                transactions.executeWithoutResult {
                    group.forEachIndexed { index, task ->
                        val error = errors[index]
                        if (error == null) {
                            if (tasks.finish(task, LocalDateTime.now(clock))) completed++
                        } else {
                            if (tasks.retry(task, error, LocalDateTime.now(clock))) failed++
                        }
                    }
                }
                if (Thread.currentThread().isInterrupted) break
            }
        } while (!Thread.currentThread().isInterrupted && System.nanoTime() - started < TimeUnit.SECONDS.toNanos(45))
        log.info("FCM 토픽 구독 동기화 완료. reconciled={}, completed={}, failed={}, pending={}",
            reconciled, completed, failed, tasks.countPending())
    }

    companion object {
        private val log = LoggerFactory.getLogger(FcmTopicSyncService::class.java)
        private const val RECONCILE_LIMIT = 1000
        private const val RECONCILE_CHUNK = 50
    }
}
