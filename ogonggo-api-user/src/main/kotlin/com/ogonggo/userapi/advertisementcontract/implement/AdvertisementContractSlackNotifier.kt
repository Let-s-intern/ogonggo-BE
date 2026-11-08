package com.ogonggo.userapi.advertisementcontract.implement

import com.ogonggo.userapi.advertisementcontract.business.AdvertisementContractNotification
import com.ogonggo.userapi.slack.adminUrl
import com.ogonggo.userapi.slack.SLACK_REST_CLIENT
import com.ogonggo.userapi.slack.SlackBlock
import com.ogonggo.userapi.slack.SlackMessage
import com.ogonggo.userapi.slack.SlackProperties
import com.ogonggo.userapi.slack.SlackText
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/** 계약 신청은 저장된 뒤 영업 Slack으로 한 번 알린다. */
@Component
internal class AdvertisementContractSlackNotifier(
    @Qualifier(SLACK_REST_CLIENT)
    private val slackRestClient: RestClient,
    private val properties: SlackProperties,
) {

    fun notify(notification: AdvertisementContractNotification) {
        check(properties.advertisementInquiry.isNotBlank()) {
            "ogonggo.slack.advertisement-inquiry가 설정되지 않았습니다."
        }

        slackRestClient.post()
            .uri(properties.advertisementInquiry)
            .body(notification.toSlackMessage(properties.adminUrl(notification.adminDetailPath())))
            .retrieve()
            .toBodilessEntity()
    }
}

/** 광고 신청 상세 화면의 확인된 프런트 경로다 (docs/prd/2026-11-07-advertisement-business-context.md:434-435). */
private fun AdvertisementContractNotification.adminDetailPath(): String = "/business/ad-applications/$contractId"

/** 영업 회신에 필요한 연락처와 접수 요약만 보낸다. 자유 입력 요청사항은 개인정보 유입 가능성이 있어 제외한다. */
private fun AdvertisementContractNotification.toSlackMessage(adminUrl: String): SlackMessage {
    val headline = "📢 광고 계약 신청 - ${SlackText.escape(companyName)}"
    return SlackMessage.withHeadline(
        headline = headline,
        blocks = listOf(
            SlackBlock.fields(
                listOf(
                    SlackText.labeled("계약 ID", contractId.toString()),
                    SlackText.labeled("기업명", companyName),
                    SlackText.labeled("공고명", recruitmentTitle),
                    SlackText.labeled("홍보 대상 · 목적", "$promotionTarget · $purpose"),
                    SlackText.labeled("신청 카테고리", categories.joinToString(" · ")),
                    SlackText.labeled("모집 마감일", recruitmentDeadline),
                    SlackText.labeled("담당자", managerName),
                    SlackText.labeled("이메일", email),
                    SlackText.labeled("전화번호", phoneNumber),
                ),
            ),
            SlackBlock.text(SlackText.link(adminUrl, "어드민에서 신청 처리")),
        ),
    )
}
