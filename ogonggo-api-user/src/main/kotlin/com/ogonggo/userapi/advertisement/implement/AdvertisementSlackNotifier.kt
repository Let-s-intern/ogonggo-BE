package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.core.error.InternalServerException
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.advertisement.error.AdvertisementErrorCode
import com.ogonggo.userapi.slack.SlackBlock
import com.ogonggo.userapi.slack.SlackMessage
import com.ogonggo.userapi.slack.SlackText
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
                .body(notification.toSlackMessage())
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
 * 기업명과 신청 유형을 요약 줄에 함께 넣는다.
 * 영업 담당자가 열어보지 않고도 바로 연락할 건인지 구분할 수 있어야 한다.
 */
private fun AdvertisementInquiryNotification.toSlackMessage(): SlackMessage =
    SlackMessage(
        text = "📢 B2B 광고 문의 - $companyName (${inquiryType.desc})",
        blocks = listOf(
            SlackBlock.fields(
                // 저장하지 않아 어드민에서 다시 볼 수 없다. 회신 수단을 함께 싣는다.
                listOf(
                    SlackText.labeled("기업명", companyName),
                    SlackText.labeled("담당자", managerName),
                    SlackText.labeled("이메일", email),
                    SlackText.labeled("전화번호", phoneNumber),
                    SlackText.labeled("신청 유형", inquiryType.desc),
                    SlackText.labeled("희망 홍보 채널", promotionChannel.desc),
                ),
            ),
            SlackBlock.text(SlackText.labeled("현재 홍보 채널 · 어려운 점", promotionAnswer)),
        ),
    )

internal const val ADVERTISEMENT_SLACK_REST_CLIENT = "advertisementSlackRestClient"
