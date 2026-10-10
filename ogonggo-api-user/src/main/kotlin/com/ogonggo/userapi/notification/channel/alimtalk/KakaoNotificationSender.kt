package com.ogonggo.userapi.notification.channel.alimtalk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.userapi.notification.delivery.implement.NotificationSender
import org.springframework.stereotype.Component

/** KAKAO 알림의 공통 발송 adapter. 개별 알림의 생성 조건·예정 시각·템플릿 값은 알지 않는다. */
@Component
internal class KakaoNotificationSender(
    private val nhnAlimTalkClient: NhnAlimTalkClient,
    private val objectMapper: ObjectMapper,
) : NotificationSender {
    override val channel = NotificationChannel.KAKAO

    override fun send(notification: NotificationMessageDto): NotificationDeliveryResult {
        // DB에 저장된 payload를 템플릿 변수 형식으로 복원한다. 주소나 payload 원문은 로그에 남기지 않는다.
        val result = nhnAlimTalkClient.send(
            AlimTalkMessage(
                recipientNo = notification.recipientAddress,
                templateCode = notification.templateCode,
                templateParameters = objectMapper.readValue(notification.payloadJson, STRING_MAP_TYPE),
                // 같은 논리 알림의 중복 호출을 NHN 멱등성 창 안에서 억제한다.
                idempotencyKey = "ogonggo:${notification.deduplicationKey}",
            ),
        )

        return when (result) {
            is NhnAlimTalkResult.Accepted -> NotificationDeliveryResult.Sent(result.requestId)
            is NhnAlimTalkResult.Error -> result.toDeliveryResult()
            NhnAlimTalkResult.InvalidResponse -> NotificationDeliveryResult.Unknown(
                resultCode = "NHN_INVALID_RESPONSE",
                failureCategory = NotificationFailureCategory.INVALID_PROVIDER_RESPONSE,
            )
        }
    }

    private fun NhnAlimTalkResult.Error.toDeliveryResult(): NotificationDeliveryResult {
        val resultCode = resultCode ?: "NHN_REJECTED_UNKNOWN_CODE"
        return when (failureCategory) {
            NotificationFailureCategory.AUTHENTICATION_CONFIGURATION,
            NotificationFailureCategory.SENDER_PROFILE_CONFIGURATION,
            NotificationFailureCategory.INVALID_REQUEST,
            NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            NotificationFailureCategory.RATE_LIMITED,
            NotificationFailureCategory.HTTP_ERROR,
            NotificationFailureCategory.CHANNEL_NOT_CONFIGURED ->
                NotificationDeliveryResult.Failed(resultCode, failureCategory)

            else -> NotificationDeliveryResult.Unknown(resultCode, failureCategory)
        }
    }

    private companion object {
        val STRING_MAP_TYPE = object : TypeReference<Map<String, String>>() {}
    }
}
