package com.ogonggo.userapi.config

import com.ogonggo.userapi.notification.channel.alimtalk.NhnAlimTalkClient
import com.ogonggo.userapi.notification.channel.alimtalk.NhnAlimTalkProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

class AlimTalkConfigurationTest {

    @Test
    fun `알림 적재 실행기와 provider 발송 실행기가 분리되어 연결된다`() {
        val context = AnnotationConfigApplicationContext()
        context.environment.propertySources.addFirst(
            MapPropertySource(
                "test-nhn-properties",
                mapOf(
                    "nhn.appKey" to "test-app-key",
                    "nhn.secretKey" to "test-secret-key",
                    "nhn.sendKey" to "test-send-key",
                    "nhn.templateCode" to "legacy-template-code",
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

        assertNotSame(metricExecutor, deliveryExecutor)
        assertNotSame(advertisementExecutor, deliveryExecutor)
        assertNotSame(deliveryExecutor, enqueueExecutor)
        assertEquals("notification-delivery-", deliveryExecutor.threadNamePrefix)
        assertEquals(4, deliveryExecutor.corePoolSize)
        assertEquals(4, deliveryExecutor.maxPoolSize)
        assertEquals(0, deliveryExecutor.threadPoolExecutor.queue.remainingCapacity())
        assertTrue(deliveryExecutor.threadPoolExecutor.rejectedExecutionHandler is ThreadPoolExecutor.AbortPolicy)
        assertEquals("notification-enqueue-", enqueueExecutor.threadNamePrefix)
        assertEquals(1, enqueueExecutor.corePoolSize)
        assertEquals(1, enqueueExecutor.maxPoolSize)
        assertEquals(1_000, enqueueExecutor.threadPoolExecutor.queue.remainingCapacity())
        assertTrue(enqueueExecutor.threadPoolExecutor.rejectedExecutionHandler is ThreadPoolExecutor.AbortPolicy)
        assertNotNull(context.getBean(NhnAlimTalkClient::class.java))
        context.close()
    }
}
