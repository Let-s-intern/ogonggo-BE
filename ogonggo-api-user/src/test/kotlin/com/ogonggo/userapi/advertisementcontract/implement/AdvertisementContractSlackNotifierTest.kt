package com.ogonggo.userapi.advertisementcontract.implement

import com.ogonggo.userapi.advertisementcontract.business.AdvertisementContractNotification
import com.ogonggo.userapi.slack.SlackProperties
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

class AdvertisementContractSlackNotifierTest {

    @Test
    @DisplayName("광고 신청 Slack 알림에 계약 ID와 상세 화면 링크를 포함한다")
    fun `영업 회신 정보와 계약 요약, 신청 상세 링크를 기존 광고 문의 webhook으로 한 번 보낸다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementContractSlackNotifier(
            builder.build(),
            SlackProperties(advertisementInquiry = WEBHOOK_URL, adminBaseUrl = ADMIN_BASE_URL),
        )

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.text").value("📢 광고 계약 신청 - 샘플기업"))
            .andExpect(jsonPath("$.blocks[1].fields[0].text").value("*계약 ID*\n1001"))
            .andExpect(jsonPath("$.blocks[1].fields[2].text").value("*공고명*\n백엔드 인턴 채용"))
            .andExpect(jsonPath("$.blocks[1].fields[6].text").value("*담당자*\n김담당"))
            .andExpect(jsonPath("$.blocks[1].fields[7].text").value("*이메일*\nmarketing@example.com"))
            .andExpect(jsonPath("$.blocks[1].fields[8].text").value("*전화번호*\n02-1234-5678"))
            .andExpect(jsonPath("$.blocks[1].fields[4].text").value("*신청 카테고리*\n배너 · 인스타그램 1"))
            .andExpect(jsonPath("$.blocks[2].text.text").value("<https://admin.example.com/business/ad-applications/1001|어드민에서 신청 처리>"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        notifier.notify(NOTIFICATION)

        // then
        server.verify()
    }

    @Test
    @DisplayName("Slack 메시지의 제어 문자를 이스케이프한다")
    fun `Slack 제어 문자가 메시지 문법으로 해석되지 않게 이스케이프한다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = AdvertisementContractSlackNotifier(builder.build(), SlackProperties(advertisementInquiry = WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.text").value("📢 광고 계약 신청 - 샘플 &amp; 기업"))
            .andExpect(jsonPath("$.blocks[1].fields[2].text").value("*공고명*\n&lt;!channel&gt; 지원자 모집"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        notifier.notify(NOTIFICATION.copy(companyName = "샘플 & 기업", recruitmentTitle = "<!channel> 지원자 모집"))

        // then
        server.verify()
    }

    @Test
    @DisplayName("webhook 미설정 또는 실패 시 예외를 호출자에게 전달한다")
    fun `webhook이 없거나 실패하면 호출자에게 예외를 돌려준다`() {
        // given
        val missingBuilder = RestClient.builder()
        val missingServer = MockRestServiceServer.bindTo(missingBuilder).build()
        val missing = AdvertisementContractSlackNotifier(missingBuilder.build(), SlackProperties())

        // when
        assertThrows(IllegalStateException::class.java) { missing.notify(NOTIFICATION) }

        // then
        missingServer.verify()

        val failingBuilder = RestClient.builder()
        val failingServer = MockRestServiceServer.bindTo(failingBuilder).build()
        val failing = AdvertisementContractSlackNotifier(failingBuilder.build(), SlackProperties(advertisementInquiry = WEBHOOK_URL))
        failingServer.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError())

        // when
        assertThrows(RestClientException::class.java) { failing.notify(NOTIFICATION) }

        // then
        failingServer.verify()
    }

    private companion object {
        const val WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/xxxx"
        const val ADMIN_BASE_URL = "https://admin.example.com"
        val NOTIFICATION = AdvertisementContractNotification(
            contractId = 1001L,
            promotionTarget = "채용 공고",
            companyName = "샘플기업",
            recruitmentTitle = "백엔드 인턴 채용",
            recruitmentDeadline = "2026-12-31",
            purpose = "적합한 지원자 확보",
            categories = listOf("배너", "인스타그램 1"),
            managerName = "김담당",
            email = "marketing@example.com",
            phoneNumber = "02-1234-5678",
        )
    }
}
