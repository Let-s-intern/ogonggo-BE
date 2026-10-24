package com.ogonggo.userapi.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskRejectedException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NotificationDeliveryTaskExecutorTest {

    @Test
    @DisplayName("플랫폼·가상 스레드 모두 발송 상한을 넘기면 큐에 쌓지 않고 거절한다")
    fun `발송 상한은 스레드 종류와 무관하게 적용한다`() {
        for (virtualThreads in listOf(false, true)) {
            // given
            val executor = NotificationDeliveryTaskExecutor(NotificationDeliveryProperties(virtualThreads, 1))
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val virtual = CompletableFuture<Boolean>()
            try {
                executor.execute {
                    virtual.complete(Thread.currentThread().isVirtual)
                    started.countDown()
                    release.await()
                }
                assertTrue(started.await(5, TimeUnit.SECONDS))

                // when / then
                assertEquals(virtualThreads, virtual.get(5, TimeUnit.SECONDS))
                assertThrows(TaskRejectedException::class.java) { executor.execute {} }
            } finally {
                release.countDown()
                executor.close()
            }
        }
    }

    @Test
    @DisplayName("종료 시 시작한 작업의 완료를 기다리고 이후 발송을 거절한다")
    fun `종료 후에는 발송 작업을 받지 않는다`() {
        // given
        val executor = NotificationDeliveryTaskExecutor(NotificationDeliveryProperties())
        val completed = CompletableFuture<Boolean>()
        executor.execute { completed.complete(true) }

        // when
        executor.close()

        // then
        assertTrue(completed.get(5, TimeUnit.SECONDS))
        assertThrows(TaskRejectedException::class.java) { executor.execute {} }
    }
}
