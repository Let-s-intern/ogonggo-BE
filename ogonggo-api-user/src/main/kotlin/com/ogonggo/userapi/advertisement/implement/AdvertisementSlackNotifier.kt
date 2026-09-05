package com.ogonggo.userapi.advertisement.implement

import com.fasterxml.jackson.annotation.JsonInclude
import com.ogonggo.core.error.InternalServerException
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.advertisement.error.AdvertisementErrorCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

interface AdvertisementInquiryNotifier {
    /** 광고 문의를 영업 채널로 알린다. 전달하지 못하면 예외를 던진다. */
    fun notify(notification: AdvertisementInquiryNotification)
}

/**
 * 슬랙 인커밍 웹훅으로 광고 문의를 보낸다.
 *
 * 실패를 삼키지 않는다. 문의를 저장하지 않아 이 메시지가 유일한 사본이므로,
 * 삼키면 사용자에게는 접수됐다고 알린 문의가 아무 데도 남지 않는다.
 * 대신 던지기 전에 원문을 error 로그로 남겨 로그에서 복구할 수 있게 한다.
 */
@Component
internal class AdvertisementSlackNotifier(
    @Qualifier(ADVERTISEMENT_SLACK_REST_CLIENT)
    private val advertisementSlackRestClient: RestClient,
    private val properties: AdvertisementSlackProperties,
) : AdvertisementInquiryNotifier {

    override fun notify(notification: AdvertisementInquiryNotification) {
        if (properties.inquiryUrl.isBlank()) {
            log.error(
                "ogonggo.advertisement.slack.inquiry-url이 설정되지 않았습니다. 유실 방지용 원문: {}",
                notification,
            )
            throw InternalServerException(AdvertisementErrorCode.ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED)
        }

        try {
            advertisementSlackRestClient.post()
                .uri(properties.inquiryUrl)
                .body(SlackMessage.from(notification))
                .retrieve()
                .toBodilessEntity()
        } catch (exception: RestClientException) {
            log.error("광고 문의 슬랙 알림 발송에 실패했습니다. 유실 방지용 원문: {}", notification, exception)
            throw InternalServerException(AdvertisementErrorCode.ADVERTISEMENT_INQUIRY_NOTIFICATION_FAILED)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AdvertisementSlackNotifier::class.java)
    }
}

/**
 * 슬랙 Block Kit 메시지다.
 *
 * text는 채널 목록과 푸시 알림에 뜨는 줄이라 기업명과 신청 유형을 함께 넣는다.
 * 영업 담당자가 열어보지 않고도 바로 연락할 건인지 구분할 수 있어야 한다.
 */
internal data class SlackMessage(
    val text: String,
    val blocks: List<SlackBlock>,
) {
    companion object {
        private const val NOTIFICATION_PREFIX = "📢 B2B 광고 문의"

        fun from(notification: AdvertisementInquiryNotification): SlackMessage {
            val summary = "$NOTIFICATION_PREFIX - ${notification.companyName} (${notification.inquiryType.desc})"
            return SlackMessage(
                text = summary,
                blocks = listOf(
                    SlackBlock.fields(
                        // 저장하지 않아 어드민에서 다시 볼 수 없다. 회신 수단을 함께 싣는다.
                        listOf(
                            SlackText.labeled("기업명", notification.companyName),
                            SlackText.labeled("담당자", notification.managerName),
                            SlackText.labeled("이메일", notification.email),
                            SlackText.labeled("전화번호", notification.phoneNumber),
                            SlackText.labeled("신청 유형", notification.inquiryType.desc),
                        ),
                    ),
                    SlackBlock.text(
                        SlackText.labeled("현재 홍보 채널 · 어려운 점", notification.promotionAnswer),
                    ),
                ),
            )
        }
    }
}

/**
 * section 블록은 text 와 fields 중 쓰는 쪽만 담아야 한다.
 * 쓰지 않는 쪽을 null 로 직렬화해 보내면 슬랙이 400 invalid_blocks 로 거절한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class SlackBlock(
    val type: String = SECTION_TYPE,
    val text: SlackText? = null,
    val fields: List<SlackText>? = null,
) {
    companion object {
        private const val SECTION_TYPE = "section"

        fun text(text: SlackText): SlackBlock = SlackBlock(text = text)

        fun fields(fields: List<SlackText>): SlackBlock = SlackBlock(fields = fields)
    }
}

internal data class SlackText(
    val type: String = MARKDOWN_TYPE,
    val text: String,
) {
    companion object {
        private const val MARKDOWN_TYPE = "mrkdwn"
        private const val EMPTY = "-"

        /** 제목을 굵게 쓰고 값을 다음 줄에 둔다. 슬랙이 필드를 두 열로 배치한다. */
        fun labeled(label: String, value: String): SlackText =
            SlackText(text = "*$label*\n${escape(value).ifBlank { EMPTY }}")

        /**
         * 슬랙 mrkdwn은 `&`, `<`, `>`를 제어 문자로 읽는다.
         * 문의자가 넣은 값이 링크나 멘션 문법으로 해석되지 않도록 먼저 치환한다.
         */
        private fun escape(value: String): String = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}

internal const val ADVERTISEMENT_SLACK_REST_CLIENT = "advertisementSlackRestClient"
