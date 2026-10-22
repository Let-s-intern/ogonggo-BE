package com.ogonggo.userapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@ConfigurationProperties(prefix = "ogonggo.notification.delivery")
data class NotificationDeliveryProperties(
    val virtualThreads: Boolean = true,
    val maxConcurrency: Int = 4,
) {
    init {
        require(maxConcurrency > 0) { "알림 발송 동시성은 양수여야 합니다." }
    }
}

/** 가상 스레드는 작업마다 생성하고, 동시 발송 수는 스레드 풀과 별도로 제한한다. */
class NotificationDeliveryTaskExecutor(properties: NotificationDeliveryProperties) : TaskExecutor, AutoCloseable {
    private val maxConcurrency = properties.maxConcurrency
    private val permits = Semaphore(maxConcurrency)
    private val executor = if (properties.virtualThreads) {
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("notification-delivery-", 0).factory())
    } else {
        Executors.newFixedThreadPool(
            maxConcurrency,
            Thread.ofPlatform().name("notification-delivery-", 0).factory(),
        )
    }

    val activeCount: Int get() = maxConcurrency - permits.availablePermits()

    override fun execute(task: Runnable) {
        if (!permits.tryAcquire()) {
            throw TaskRejectedException("알림 발송 동시성 상한에 도달했습니다.")
        }
        try {
            executor.execute {
                try {
                    task.run()
                } finally {
                    permits.release()
                }
            }
        } catch (exception: RejectedExecutionException) {
            permits.release()
            throw TaskRejectedException("알림 발송 실행기가 종료됐습니다.", exception)
        }
    }

    override fun close() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow()
        } catch (_: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
