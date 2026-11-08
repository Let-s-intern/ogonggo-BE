package com.ogonggo.userapi.config

import com.ogonggo.userapi.advertisementcontract.business.AdvertisementContractNotification
import com.ogonggo.userapi.advertisementcontract.implement.AdvertisementContractMailSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AdvertisementContractTaskExecutorConfigurationTest {

    @Test
    @DisplayName("계약 메일은 별도 AbortPolicy 실행기를 쓰고 광고 문의 CallerRuns 정책은 유지한다")
    fun `계약 메일 실행기는 포화 시 요청 스레드에서 실행하지 않는다`() {
        // given
        val configuration = UserAsyncConfiguration()
        val inquiryExecutor = configuration.advertisementTaskExecutor().apply { afterPropertiesSet() }
        val contractExecutor = configuration.advertisementContractTaskExecutor().apply { afterPropertiesSet() }
        val releaseWorkers = CountDownLatch(1)

        try {
            assertNotSame(inquiryExecutor, contractExecutor)
            assertTrue(inquiryExecutor.threadPoolExecutor.rejectedExecutionHandler is ThreadPoolExecutor.CallerRunsPolicy)
            assertTrue(contractExecutor.threadPoolExecutor.rejectedExecutionHandler is ThreadPoolExecutor.AbortPolicy)
            assertEquals(100, contractExecutor.threadPoolExecutor.queue.remainingCapacity())
            assertEquals(
                UserAsyncConfiguration.ADVERTISEMENT_CONTRACT_TASK_EXECUTOR,
                AdvertisementContractMailSender::class.java
                    .getMethod("sendConfirmation", AdvertisementContractNotification::class.java)
                    .getAnnotation(Async::class.java)
                    .value,
            )

            val runningWorkers = CountDownLatch(contractExecutor.maxPoolSize)
            val blockedTask = Runnable {
                runningWorkers.countDown()
                releaseWorkers.await()
            }
            val pool = contractExecutor.threadPoolExecutor

            // 코어 worker 하나와 큐를 채운 뒤 최대 worker를 추가로 점유해 실제 포화를 만든다.
            pool.execute(blockedTask)
            repeat(contractExecutor.threadPoolExecutor.queue.remainingCapacity()) { pool.execute(blockedTask) }
            pool.execute(blockedTask)
            assertTrue(runningWorkers.await(5, TimeUnit.SECONDS))
            assertEquals(100, pool.queue.size)

            val rejectedTaskThread = AtomicReference<Thread?>()

            // when
            assertThrows(RejectedExecutionException::class.java) {
                pool.execute { rejectedTaskThread.set(Thread.currentThread()) }
            }

            // then
            assertNull(rejectedTaskThread.get())
        } finally {
            releaseWorkers.countDown()
            contractExecutor.shutdown()
            inquiryExecutor.shutdown()
        }
    }
}
