package com.ogonggo.userapi.notification.channel.alimtalk

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class KakaoNotificationSenderTest {

    @Test
    @DisplayName("분류되지 않은 NHN 오류는 접수 여부 미확정으로 남긴다")
    fun `미등록 오류 코드를 미확정 결과로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage()))
            .thenReturn(
                NhnAlimTalkResult.Error(
                    "400101",
                    NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR,
                    "매핑되지 않은 NHN API 오류 코드",
                ),
            )

        val result = sender.send(notification())

        assertEquals(
            NotificationDeliveryResult.Unknown("400101", NotificationFailureCategory.UNKNOWN_PROVIDER_ERROR),
            result,
        )
    }

    @Test
    @DisplayName("템플릿 설정 오류처럼 명시적으로 거절된 요청은 FAILED로 남긴다")
    fun `명시적 템플릿 거절을 실패로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage())).thenReturn(
            NhnAlimTalkResult.Error(
                "-3005",
                NotificationFailureCategory.TEMPLATE_CONFIGURATION,
                "템플릿 승인 상태 오류",
            ),
        )

        val result = sender.send(notification())

        assertEquals(
            NotificationDeliveryResult.Failed("-3005", NotificationFailureCategory.TEMPLATE_CONFIGURATION),
            result,
        )
    }

    @Test
    @DisplayName("provider 5xx 응답은 처리 결과가 불명확하므로 UNKNOWN으로 남긴다")
    fun `provider 오류를 미확정 결과로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage())).thenReturn(
            NhnAlimTalkResult.Error(
                "NHN_HTTP_500",
                NotificationFailureCategory.PROVIDER_UNAVAILABLE,
                "NHN HTTP 응답 오류 (500)",
            ),
        )

        val result = sender.send(notification())

        assertEquals(
            NotificationDeliveryResult.Unknown("NHN_HTTP_500", NotificationFailureCategory.PROVIDER_UNAVAILABLE),
            result,
        )
    }

    @Test
    @DisplayName("NHN 응답을 받지 못한 통신 오류는 접수 여부 미확정으로 반환한다")
    fun `통신 실패를 미확정 결과로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage()))
            .thenReturn(
                NhnAlimTalkResult.Error(
                    "NHN_TRANSPORT_ERROR",
                    NotificationFailureCategory.TRANSPORT_ERROR,
                    "NHN 응답을 받기 전 통신 오류",
                ),
            )

        val result = sender.send(notification())

        assertEquals(NotificationDeliveryResult.Unknown("NHN_TRANSPORT_ERROR", NotificationFailureCategory.TRANSPORT_ERROR), result)
    }

    @Test
    @DisplayName("NHN 중복 멱등성 키 응답은 접수 여부 미확정으로 반환한다")
    fun `중복 키 응답을 미확정 결과로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage())).thenReturn(
            NhnAlimTalkResult.Error(
                "-1005",
                NotificationFailureCategory.DUPLICATE_REQUEST,
                "동일한 멱등성 키로 10분 이내 요청됨. 이전 요청의 최종 전달 여부는 별도 확인 필요",
            ),
        )

        val result = sender.send(notification())

        assertEquals(NotificationDeliveryResult.Unknown("-1005", NotificationFailureCategory.DUPLICATE_REQUEST), result)
    }

    @Test
    @DisplayName("NHN 응답을 해석할 수 없으면 접수 여부 미확정으로 반환한다")
    fun `불완전 응답은 미확정 결과로 변환한다`() {
        val client = Mockito.mock(NhnAlimTalkClient::class.java)
        val sender = KakaoNotificationSender(client, ObjectMapper())
        Mockito.`when`(client.send(expectedMessage())).thenReturn(NhnAlimTalkResult.InvalidResponse)

        val result = sender.send(notification())

        assertEquals(
            NotificationDeliveryResult.Unknown("NHN_INVALID_RESPONSE", NotificationFailureCategory.INVALID_PROVIDER_RESPONSE),
            result,
        )
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
