package com.ogonggo.core.notificationbatch.implement

import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * enabled는 ALL 전환 게이트다. 구독 동기화는 topic 설정만으로 먼저 수행한다.
 *
 * 일반회원(generalTopic)·기업회원(companyTopic) 토픽도 같은 방식이다. 토픽 이름만 설정하면 구독 동기화가 먼저 돌고,
 * memberTopicsEnabled를 켜야 관리자 발송이 이 토픽으로 보낸다. 끄면 해당 회원의 기기마다 보낸다.
 */
@ConfigurationProperties("ogonggo.notification.broadcast")
data class NotificationBroadcastProperties(
    val enabled: Boolean = false,
    val topic: String = "",
    val memberTopicsEnabled: Boolean = false,
    val generalTopic: String = "",
    val companyTopic: String = "",
) {
    init {
        listOf(topic, generalTopic, companyTopic).forEach {
            require(it.isEmpty() || (it.length <= 200 && it.matches(Regex("[a-zA-Z0-9-_.~%]+"))))
        }
        require(!enabled || topic.isNotEmpty()) { "전체 토픽 발송을 켜려면 topic이 필요합니다." }
        require(!memberTopicsEnabled || (generalTopic.isNotEmpty() && companyTopic.isNotEmpty())) {
            "회원 종류 토픽 발송을 켜려면 generalTopic과 companyTopic이 모두 필요합니다."
        }
        val configured = syncTopics()
        require(configured.size == configured.distinct().size) { "토픽 이름은 서로 달라야 합니다." }
    }

    /** 이름이 설정되어 구독 동기화 대상인 토픽과 그 토픽을 받을 대상. */
    fun topicAudiences(): Map<String, NotificationBatchTargetType> = buildMap {
        if (topic.isNotEmpty()) put(topic, NotificationBatchTargetType.ALL)
        if (generalTopic.isNotEmpty()) put(generalTopic, NotificationBatchTargetType.GENERAL_MEMBERS)
        if (companyTopic.isNotEmpty()) put(companyTopic, NotificationBatchTargetType.COMPANY_MEMBERS)
    }

    fun syncTopics(): List<String> = listOf(topic, generalTopic, companyTopic).filter { it.isNotEmpty() }

    /** 관리자 발송이 토픽으로 보낼 때 쓸 토픽. 해당 게이트가 꺼져 있거나 토픽 발송 대상이 아니면 null이다. */
    fun sendingTopicFor(target: NotificationBatchTargetType): String? = when (target) {
        NotificationBatchTargetType.ALL -> topic.takeIf { enabled }
        NotificationBatchTargetType.GENERAL_MEMBERS -> generalTopic.takeIf { memberTopicsEnabled }
        NotificationBatchTargetType.COMPANY_MEMBERS -> companyTopic.takeIf { memberTopicsEnabled }
        NotificationBatchTargetType.USER_IDS, NotificationBatchTargetType.JOB_ROLES -> null
    }
}

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NotificationBroadcastProperties::class)
internal class NotificationBroadcastConfiguration
