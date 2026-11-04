package com.ogonggo.core.notification

import com.ogonggo.core.notificationbatch.domain.NotificationBatchTargetType
import com.ogonggo.core.notificationbatch.implement.NotificationBroadcastProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 관리자 발송이 어느 대상을 토픽으로 보낼지 정하는 설정 규칙을 확인한다. */
class NotificationBroadcastPropertiesTest {
    @Test
    @DisplayName("회원 종류 토픽 이름만 설정하면 구독 동기화 대상이 되지만 발송은 기기마다 한다")
    fun `회원 종류 토픽은 게이트를 켜야 토픽으로 보낸다`() {
        // given
        val properties = NotificationBroadcastProperties(
            enabled = true, topic = "all", generalTopic = "general", companyTopic = "company",
        )
        // when / then
        assertEquals(listOf("all", "general", "company"), properties.syncTopics())
        assertEquals("all", properties.sendingTopicFor(NotificationBatchTargetType.ALL))
        assertNull(properties.sendingTopicFor(NotificationBatchTargetType.GENERAL_MEMBERS))
        assertNull(properties.sendingTopicFor(NotificationBatchTargetType.COMPANY_MEMBERS))
    }

    @Test
    @DisplayName("회원 종류 토픽 게이트를 켜면 일반·기업 대상은 각자의 토픽으로 보내고 지정 회원 발송은 토픽을 쓰지 않는다")
    fun `회원 종류 토픽 게이트를 켜면 해당 토픽으로 보낸다`() {
        // given
        val properties = NotificationBroadcastProperties(
            topic = "all", memberTopicsEnabled = true, generalTopic = "general", companyTopic = "company",
        )
        // when / then
        assertNull(properties.sendingTopicFor(NotificationBatchTargetType.ALL))
        assertEquals("general", properties.sendingTopicFor(NotificationBatchTargetType.GENERAL_MEMBERS))
        assertEquals("company", properties.sendingTopicFor(NotificationBatchTargetType.COMPANY_MEMBERS))
        assertNull(properties.sendingTopicFor(NotificationBatchTargetType.USER_IDS))
    }

    @Test
    @DisplayName("회원 종류 토픽 게이트는 두 토픽 이름이 모두 있어야 켤 수 있고 토픽 이름은 서로 달라야 한다")
    fun `잘못된 회원 종류 토픽 설정은 기동 시 거부한다`() {
        assertThrows<IllegalArgumentException> {
            NotificationBroadcastProperties(memberTopicsEnabled = true, generalTopic = "general")
        }
        assertThrows<IllegalArgumentException> {
            NotificationBroadcastProperties(topic = "same", generalTopic = "same")
        }
    }
}
