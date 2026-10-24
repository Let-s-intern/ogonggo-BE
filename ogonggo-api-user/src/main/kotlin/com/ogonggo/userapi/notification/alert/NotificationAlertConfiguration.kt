package com.ogonggo.userapi.notification.alert

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationAlertProperties::class)
class NotificationAlertConfiguration {
    @Bean(name = [NOTIFICATION_ALERT_TASK_EXECUTOR])
    fun notificationAlertTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 1
        queueCapacity = 100
        setThreadNamePrefix("notification-alert-")
        setRejectedExecutionHandler(ThreadPoolExecutor.AbortPolicy())
        setWaitForTasksToCompleteOnShutdown(true)
        setAwaitTerminationSeconds(5)
    }
}

internal const val NOTIFICATION_ALERT_TASK_EXECUTOR = "notificationAlertTaskExecutor"
