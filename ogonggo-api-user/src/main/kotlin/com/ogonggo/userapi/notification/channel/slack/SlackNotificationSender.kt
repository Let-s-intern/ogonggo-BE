package com.ogonggo.userapi.notification.channel.slack

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.domain.NotificationFailureCategory
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.userapi.notification.delivery.implement.NotificationSender
import com.ogonggo.userapi.slack.SLACK_REST_CLIENT
import com.ogonggo.userapi.slack.SlackBlock
import com.ogonggo.userapi.slack.SlackMessage
import com.ogonggo.userapi.slack.SlackProperties
import com.ogonggo.userapi.slack.SlackText
import com.ogonggo.userapi.slack.adminUrl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

/** Slack incoming webhook으로 알림 대기열의 메시지를 발송한다. */
@Component
internal class SlackNotificationSender(
    @Qualifier(SLACK_REST_CLIENT)
    private val slackRestClient: RestClient,
    private val properties: SlackProperties,
    private val objectMapper: ObjectMapper,
) : NotificationSender {
    override val channel = NotificationChannel.SLACK

    override fun send(notification: NotificationMessageDto): NotificationDeliveryResult {
        val webhook = when (notification.recipientAddress) {
            ADVERTISEMENT_INQUIRY_ADDRESS -> properties.advertisementInquiry
            else -> return NotificationDeliveryResult.Failed(
                resultCode = "UNKNOWN_SLACK_RECIPIENT",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
            )
        }
        if (webhook.isBlank()) return NotificationDeliveryResult.Failed(
            resultCode = "SLACK_WEBHOOK_NOT_CONFIGURED",
            failureCategory = NotificationFailureCategory.CHANNEL_NOT_CONFIGURED,
        )

        val parameters = try {
            objectMapper.readValue(notification.payloadJson, STRING_MAP_TYPE)
        } catch (_: Exception) {
            return NotificationDeliveryResult.Failed(
                resultCode = "INVALID_SLACK_PAYLOAD",
                failureCategory = NotificationFailureCategory.INVALID_REQUEST,
            )
        }
        val message = when (notification.templateCode) {
            ADVERTISEMENT_CONTRACT_TEMPLATE_CODE -> parameters.toAdvertisementContractMessage()
                ?: return NotificationDeliveryResult.Failed(
                    resultCode = "INVALID_SLACK_TEMPLATE_PARAMETERS",
                    failureCategory = NotificationFailureCategory.INVALID_REQUEST,
                )
            else -> return NotificationDeliveryResult.Failed(
                resultCode = "UNKNOWN_SLACK_TEMPLATE",
                failureCategory = NotificationFailureCategory.TEMPLATE_CONFIGURATION,
            )
        }

        return try {
            slackRestClient.post().uri(webhook).body(message).retrieve().toBodilessEntity()
            NotificationDeliveryResult.Sent(null)
        } catch (exception: RestClientResponseException) {
            NotificationDeliveryResult.Failed(
                resultCode = "SLACK_HTTP_${exception.statusCode.value()}",
                failureCategory = NotificationFailureCategory.HTTP_ERROR,
            )
        } catch (_: ResourceAccessException) {
            NotificationDeliveryResult.Unknown(
                resultCode = "SLACK_TRANSPORT_ERROR",
                failureCategory = NotificationFailureCategory.TRANSPORT_ERROR,
            )
        } catch (_: RestClientException) {
            NotificationDeliveryResult.Unknown(
                resultCode = "SLACK_REQUEST_ERROR",
                failureCategory = NotificationFailureCategory.TRANSPORT_ERROR,
            )
        }
    }

    private fun Map<String, String>.toAdvertisementContractMessage(): SlackMessage? {
        val contractId = get("contractId") ?: return null
        val companyName = get("companyName") ?: return null
        val recruitmentTitle = get("recruitmentTitle") ?: return null
        val promotionTarget = get("promotionTarget") ?: return null
        val purpose = get("purpose") ?: return null
        val categories = get("categories") ?: return null
        val recruitmentDeadline = get("recruitmentDeadline") ?: return null
        val managerName = get("managerName") ?: return null
        val email = get("email") ?: return null
        val phoneNumber = get("phoneNumber") ?: return null
        val budgetRange = get("budgetRange")
        val adminUrl = properties.adminUrl("/business/ad-applications/$contractId")
        return SlackMessage.withHeadline(
            headline = "📢 광고 계약 신청 - ${SlackText.escape(companyName)}",
            blocks = listOf(
                SlackBlock.fields(
                    listOf(
                        SlackText.labeled("계약 ID", contractId),
                        SlackText.labeled("기업명", companyName),
                        SlackText.labeled("공고명", recruitmentTitle),
                        SlackText.labeled("홍보 대상 · 목적", "$promotionTarget · $purpose"),
                        SlackText.labeled("신청 카테고리", categories),
                        SlackText.labeled("모집 마감일", recruitmentDeadline),
                        SlackText.labeled("담당자", managerName),
                        SlackText.labeled("이메일", email),
                        SlackText.labeled("전화번호", phoneNumber),
                    ) + listOfNotNull(budgetRange?.let { SlackText.labeled("예정 예산", it) }),
                ),
                SlackBlock.text(SlackText.link(adminUrl, "어드민에서 신청 처리")),
            ),
        )
    }

    private companion object {
        const val ADVERTISEMENT_INQUIRY_ADDRESS = "advertisement-inquiry"
        const val ADVERTISEMENT_CONTRACT_TEMPLATE_CODE = "advertisement_contract_submitted"
        val STRING_MAP_TYPE = object : TypeReference<Map<String, String>>() {}
    }
}
