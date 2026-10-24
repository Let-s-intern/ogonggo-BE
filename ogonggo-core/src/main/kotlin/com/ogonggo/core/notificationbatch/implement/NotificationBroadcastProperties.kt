package com.ogonggo.core.notificationbatch.implement

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/** enabled는 ALL 전환 게이트다. 구독 동기화는 topic 설정만으로 먼저 수행한다. */
@ConfigurationProperties("ogonggo.notification.broadcast")
data class NotificationBroadcastProperties(val enabled: Boolean = false, val topic: String = "") {
    init {
        require(topic.isEmpty() || (topic.length <= 200 && topic.matches(Regex("[a-zA-Z0-9-_.~%]+"))))
        require(!enabled || topic.isNotEmpty()) { "전체 토픽 발송을 켜려면 topic이 필요합니다." }
    }
}

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationBroadcastProperties::class)
internal class NotificationBroadcastConfiguration
