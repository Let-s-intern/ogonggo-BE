package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.slack.SlackBlock
import com.ogonggo.userapi.slack.SlackMessage
import com.ogonggo.userapi.slack.SlackText
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

interface AdvertisementInquiryNotifier {
    /** 광고 문의를 영업 채널로 알린다. 전달하지 못하면 예외를 던지고, 접수에 미칠 영향은 호출한 쪽이 정한다. */
    fun notify(notification: AdvertisementInquiryNotification)
}

/**
 * 슬랙 인커밍 웹훅으로 광고 문의를 보낸다.
 *
 * 실패를 여기서 삼키지 않고 던진다. 문의는 이미 저장돼 있으므로 서비스가 로그만 남기고 접수를 마친다.
 */
@Component
internal class AdvertisementSlackNotifier(
    @Qualifier(ADVERTISEMENT_SLACK_REST_CLIENT)
    private val advertisementSlackRestClient: RestClient,
    private val properties: AdvertisementSlackProperties,
) : AdvertisementInquiryNotifier {

    override fun notify(notification: AdvertisementInquiryNotification) {
        // 값이 없어도 애플리케이션은 기동하도록 두었으므로 보낼 때 실패로 알린다.
        check(properties.inquiryUrl.isNotBlank()) { "ogonggo.advertisement.slack.inquiry-url이 설정되지 않았습니다." }

        advertisementSlackRestClient.post()
            .uri(properties.inquiryUrl)
            .body(notification.toSlackMessage())
            .retrieve()
            .toBodilessEntity()
    }
}

/**
 * 기업명과 신청 유형을 요약 줄에 함께 넣는다.
 * 영업 담당자가 열어보지 않고도 바로 연락할 건인지 구분할 수 있어야 한다.
 */
private fun AdvertisementInquiryNotification.toSlackMessage(): SlackMessage =
    SlackMessage.withHeadline(
        headline = "📢 B2B 광고 문의 - ${SlackText.escape(companyName)} (${inquiryType.desc})",
        blocks = listOf(
            SlackBlock.fields(
                // 영업이 슬랙만 보고 바로 연락할 수 있도록 회신 수단을 함께 싣는다.
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
