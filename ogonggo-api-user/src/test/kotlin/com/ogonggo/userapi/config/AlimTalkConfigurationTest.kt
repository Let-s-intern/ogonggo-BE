package com.ogonggo.userapi.config

import com.ogonggo.userapi.notification.channel.alimtalk.NhnAlimTalkClient
import com.ogonggo.userapi.notification.channel.alimtalk.NhnAlimTalkProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class AlimTalkConfigurationTest {

    @Test
    @DisplayName("발송은 가상 스레드에서 실행하고 적재 실행기와 분리한다")
    fun `알림 적재 실행기와 provider 발송 실행기가 분리되어 연결된다`() {
        // given
        val context = AnnotationConfigApplicationContext()
        context.beanFactory.registerSingleton("meterRegistry", SimpleMeterRegistry())
        context.environment.propertySources.addFirst(
            MapPropertySource(
                "test-nhn-properties",
                mapOf(
                    "nhn.appKey" to "test-app-key",
                    "nhn.secretKey" to "test-secret-key",
                    "nhn.sendKey" to "test-send-key",
                    "nhn.templateCode" to "legacy-template-code",
                    "ogonggo.notification.delivery.max-concurrency" to "16",
                    "ogonggo.notification.delivery.db-workers" to "4",
                    "ogonggo.notification.delivery.db-queue-capacity" to "16",
                ),
            ),
        )
        context.register(
            UserAsyncConfiguration::class.java,
            UserAlimTalkConfiguration::class.java,
            NhnAlimTalkProperties::class.java,
            NhnAlimTalkClient::class.java,
        )
        context.refresh()

        val deliveryExecutor = context.getBean(
            UserAsyncConfiguration.NOTIFICATION_DELIVERY_TASK_EXECUTOR,
            NotificationDeliveryTaskExecutor::class.java,
        )
        val resultDbExecutor = context.getBean(
            UserAsyncConfiguration.NOTIFICATION_RESULT_DB_TASK_EXECUTOR,
            ThreadPoolTaskExecutor::class.java,
        )
        val enqueueExecutor = context.getBean(
            UserAsyncConfiguration.NOTIFICATION_ENQUEUE_TASK_EXECUTOR,
            ThreadPoolTaskExecutor::class.java,
        )
        val metricExecutor = context.getBean(
            UserAsyncConfiguration.METRIC_TASK_EXECUTOR,
            ThreadPoolTaskExecutor::class.java,
        )
        val advertisementExecutor = context.getBean(
            UserAsyncConfiguration.ADVERTISEMENT_TASK_EXECUTOR,
            ThreadPoolTaskExecutor::class.java,
        )

        // when
        val deliveryThread = CompletableFuture<Thread>()
        deliveryExecutor.execute { deliveryThread.complete(Thread.currentThread()) }
        val actualThread = deliveryThread.get(5, TimeUnit.SECONDS)

        // then
        assertTrue(actualThread.isVirtual)
        assertTrue(actualThread.name.startsWith("notification-delivery-"))
        assertNotSame(metricExecutor, deliveryExecutor)
        assertNotSame(advertisementExecutor, deliveryExecutor)
        assertNotSame(deliveryExecutor, enqueueExecutor)
        assertNotSame(deliveryExecutor, resultDbExecutor)
        assertEquals("notification-enqueue-", enqueueExecutor.threadNamePrefix)
        assertEquals(1, enqueueExecutor.corePoolSize)
        assertEquals(1, enqueueExecutor.maxPoolSize)
        assertEquals(1_000, enqueueExecutor.threadPoolExecutor.queue.remainingCapacity())
        assertTrue(enqueueExecutor.threadPoolExecutor.rejectedExecutionHandler is ThreadPoolExecutor.AbortPolicy)
        assertEquals(4, resultDbExecutor.corePoolSize)
        assertEquals(4, resultDbExecutor.maxPoolSize)
        assertEquals(16, resultDbExecutor.threadPoolExecutor.queue.remainingCapacity())
        assertTrue(resultDbExecutor.threadPoolExecutor.rejectedExecutionHandler is ThreadPoolExecutor.AbortPolicy)
        assertNotNull(context.getBean(NhnAlimTalkClient::class.java))
        context.close()
    }
}
