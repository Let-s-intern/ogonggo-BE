package com.ogonggo.userapi.advertisementcontract.implement

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.NotificationAppender
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import com.ogonggo.userapi.advertisementcontract.implement.dto.AdvertisementContractNotificationDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/** 신청 저장 뒤 이메일과 영업 Slack 알림을 공통 발송 대기열에 함께 적재한다. */
@Component
internal class AdvertisementContractNotificationAppender(
    private val notificationAppender: NotificationAppender,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun append(notification: AdvertisementContractNotificationDto): Int {
        val payloadJson = objectMapper.writeValueAsString(notification.templateParameters())
        val scheduledAt = LocalDateTime.now(clock)
        return notificationAppender.appendAll(
            listOf(
                NotificationAppendDto(
                    deduplicationKey = "advertisement-contract:${notification.contractId}:email",
                    channel = NotificationChannel.EMAIL,
                    templateCode = EMAIL_TEMPLATE_CODE,
                    recipientAddress = notification.email,
                    payloadJson = payloadJson,
                    scheduledAt = scheduledAt,
                ),
                NotificationAppendDto(
                    deduplicationKey = "advertisement-contract:${notification.contractId}:slack",
                    channel = NotificationChannel.SLACK,
                    templateCode = SLACK_TEMPLATE_CODE,
                    recipientAddress = ADVERTISEMENT_INQUIRY_ADDRESS,
                    payloadJson = payloadJson,
                    scheduledAt = scheduledAt,
                ),
            ),
        )
    }

    private companion object {
        const val EMAIL_TEMPLATE_CODE = "advertisement_contract_received"
        const val SLACK_TEMPLATE_CODE = "advertisement_contract_submitted"
        const val ADVERTISEMENT_INQUIRY_ADDRESS = "advertisement-inquiry"
    }
}
