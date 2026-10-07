package com.ogonggo.userapi.notification.channel.alimtalk

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class KakaoNotificationSenderTest {

    @Test
    @DisplayName("NHN 명시 거절은 실패 사유를 보존하고 재시도하지 않는다")
    fun `명시 거절을 재시도 불가 실패로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage()))
            .thenReturn(NhnAlimTalkResult.Rejected("400101"))

        val result = sender.send(notification())

        assertEquals(NotificationDeliveryResult.Failed("400101"), result)
    }

    @Test
    @DisplayName("응답을 받지 못한 통신 오류도 재시도하지 않고 실패로 반환한다")
    fun `통신 실패를 최종 실패로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage()))
            .thenReturn(NhnAlimTalkResult.Rejected("NHN_TRANSPORT_ERROR"))

        val result = sender.send(notification())

        assertEquals(NotificationDeliveryResult.Failed("NHN_TRANSPORT_ERROR"), result)
    }

    @Test
    @DisplayName("NHN 중복 멱등성 키 응답은 결과 코드와 함께 실패로 기록한다")
    fun `중복 키 응답을 실패로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage())).thenReturn(NhnAlimTalkResult.DuplicateIdempotencyKey)

        val result = sender.send(notification())

        assertEquals(NotificationDeliveryResult.Failed("-1005"), result)
    }

    @Test
    @DisplayName("NHN 응답을 분류할 수 없으면 재시도하지 않고 실패 사유를 남긴다")
    fun `불완전 응답은 재시도 불가 실패로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage())).thenReturn(NhnAlimTalkResult.InvalidResponse)

        val result = sender.send(notification())

        assertEquals(NotificationDeliveryResult.Failed("NHN_INVALID_RESPONSE"), result)
    }

    @Test
    @DisplayName("공통 알림 행의 templateCode·payload·dedup key를 NHN 요청으로 변환한다")
    fun `저장된 템플릿 정보로 NHN 요청을 구성한다`() {
        // given
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        val notification = notification()
        val expectedMessage = AlimTalkMessage(
            recipientNo = "01012345678",
            templateCode = "sign_up_confirm",
            templateParameters = mapOf("name" to "홍길동", "userEmail" to "user@example.com"),
            idempotencyKey = "ogonggo:sign_up_confirm:user:17:KAKAO",
        )
        Mockito.`when`(client.send(expectedMessage)).thenReturn(NhnAlimTalkResult.Accepted("request-1"))

        // when
        val result = sender.send(notification)

        // then
        assertEquals(NotificationDeliveryResult.Sent("request-1"), result)
        Mockito.verify(client).send(expectedMessage)
    }

    private fun notification() = NotificationMessageDto(
        notificationId = 1,
        channel = NotificationChannel.KAKAO,
        templateCode = "sign_up_confirm",
        recipientAddress = "01012345678",
        payloadJson = "{\"name\":\"홍길동\",\"userEmail\":\"user@example.com\"}",
        deduplicationKey = "sign_up_confirm:user:17:KAKAO",
    )

    private fun expectedMessage() = AlimTalkMessage(
        recipientNo = "01012345678",
        templateCode = "sign_up_confirm",
        templateParameters = mapOf("name" to "홍길동", "userEmail" to "user@example.com"),
        idempotencyKey = "ogonggo:sign_up_confirm:user:17:KAKAO",
    )
}
