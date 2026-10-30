package com.ogonggo.userapi.advertisement.implement

import com.ogonggo.core.advertisementinquiry.domain.AdvertisementInquiryInflowChannel
import com.ogonggo.userapi.advertisement.business.AdvertisementInquiryNotification
import com.ogonggo.userapi.slack.SlackBlock
import com.ogonggo.userapi.slack.SLACK_REST_CLIENT
import com.ogonggo.userapi.slack.SlackMessage
import com.ogonggo.userapi.slack.SlackProperties
import com.ogonggo.userapi.slack.SlackText
import com.ogonggo.userapi.slack.adminUrl
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
    @Qualifier(SLACK_REST_CLIENT)
    private val slackRestClient: RestClient,
    private val properties: SlackProperties,
) : AdvertisementInquiryNotifier {

    override fun notify(notification: AdvertisementInquiryNotification) {
        // 값이 없어도 애플리케이션은 기동하도록 두었으므로 보낼 때 실패로 알린다.
        check(properties.advertisementInquiry.isNotBlank()) { "ogonggo.slack.advertisement-inquiry가 설정되지 않았습니다." }

        slackRestClient.post()
            .uri(properties.advertisementInquiry)
            .body(notification.toSlackMessage(properties.adminUrl(ADMIN_INQUIRY_LIST_PATH)))
            .retrieve()
            .toBodilessEntity()
    }
}

/**
 * 기업명과 신청 유형을 요약 줄에 함께 넣는다.
 * 영업 담당자가 열어보지 않고도 바로 연락할 건인지 구분할 수 있어야 한다.
 */
private fun AdvertisementInquiryNotification.toSlackMessage(adminUrl: String): SlackMessage =
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
            // 영업이 슬랙만 보고도 어디서 온 신청인지 알 수 있게 한다.
            SlackBlock.text(SlackText.labeled("회원 구분 · 유입 경로", "${memberType.desc} · ${inflowLabel()}")),
            SlackBlock.text(SlackText.link(adminUrl, "어드민에서 보기")),
        ),
    )

/**
 * 판정한 유입 경로 이름 뒤에 영업이 링크를 나눠 쓴 UTM 값을 붙인다.
 * 정해 두지 않은 utm_source는 원문을, 외부 사이트는 referrer 호스트를 이름 자리에 쓴다.
 */
private fun AdvertisementInquiryNotification.inflowLabel(): String {
    val name = when (inflowChannel) {
        AdvertisementInquiryInflowChannel.UTM_OTHER -> utmSource?.trim().orEmpty()
        AdvertisementInquiryInflowChannel.EXTERNAL_SITE ->
            AdvertisementInquiryInflowChannel.referrerHost(referrer)
                ?.let { "${inflowChannel.desc} · $it" }
                ?: inflowChannel.desc
        else -> inflowChannel.desc
    }
    val details = listOfNotNull(
        utmCampaign?.takeIf { it.isNotBlank() }?.let { "utm_campaign=$it" },
        utmContent?.takeIf { it.isNotBlank() }?.let { "utm_content=$it" },
    )
    return (listOf(name) + details).joinToString(" · ")
}

/**
 * 어드민 광고 문의 목록 경로다. 어드민에 문의 상세 화면이 없어 목록으로 보낸다.
 * 어드민이 문의 ID로 특정 건을 열 수 있게 되면 여기에 ID를 붙인다.
 */
private const val ADMIN_INQUIRY_LIST_PATH = "/business/ad-inquiries"
