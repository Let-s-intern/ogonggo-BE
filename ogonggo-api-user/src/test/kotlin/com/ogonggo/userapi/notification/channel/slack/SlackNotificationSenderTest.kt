package com.ogonggo.userapi.notification.channel.slack

import com.fasterxml.jackson.databind.ObjectMapper
import com.ogonggo.core.notification.delivery.implement.dto.NotificationDeliveryResult
import com.ogonggo.core.notification.delivery.implement.dto.NotificationMessageDto
import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.userapi.slack.SlackProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class SlackNotificationSenderTest {

    @Test
    @DisplayName("광고 계약 Slack 알림이 전체 신청 요약과 어드민 링크를 보낸다")
    fun `공고명 홍보 대상 목적 마감일과 회신 정보를 포함한다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val sender = SlackNotificationSender(
            builder.build(),
            SlackProperties(advertisementInquiry = WEBHOOK_URL, adminBaseUrl = ADMIN_BASE_URL),
            ObjectMapper(),
        )
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.text").value("📢 광고 계약 신청 - 샘플기업"))
            .andExpect(jsonPath("$.blocks[1].fields[2].text").value("*공고명*\n백엔드 인턴 채용"))
            .andExpect(jsonPath("$.blocks[1].fields[3].text").value("*홍보 대상 · 목적*\n채용 공고 · 적합한 지원자 확보"))
            .andExpect(jsonPath("$.blocks[1].fields[4].text").value("*신청 카테고리*\n배너 · 인스타그램 1"))
            .andExpect(jsonPath("$.blocks[1].fields[5].text").value("*모집 마감일*\n2026-12-31"))
            .andExpect(jsonPath("$.blocks[1].fields[7].text").value("*이메일*\nmarketing@example.com"))
            .andExpect(jsonPath("$.blocks[2].text.text").value("<https://admin.example.com/business/ad-applications/1001|어드민에서 신청 처리>"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        val result = sender.send(NOTIFICATION)

        // then
        assertEquals(NotificationDeliveryResult.Sent(null), result)
        server.verify()
    }

    @Test
    @DisplayName("광고 계약 Slack 알림은 신청자가 보낸 사업자 번호를 전화번호 뒤에 붙이고, 없으면 칸을 만들지 않는다")
    fun `사업자 번호가 있을 때만 사업자 번호 칸을 보낸다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val sender = SlackNotificationSender(
            builder.build(),
            SlackProperties(advertisementInquiry = WEBHOOK_URL),
            ObjectMapper(),
        )
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.blocks[1].fields.length()").value(10))
            .andExpect(jsonPath("$.blocks[1].fields[9].text").value("*사업자 번호*\n사업자등록번호 1234567890"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.blocks[1].fields.length()").value(9))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        val withNumber = sender.send(
            NOTIFICATION.copy(
                payloadJson = ObjectMapper().writeValueAsString(PARAMETERS + ("businessNumber" to "사업자등록번호 1234567890")),
            ),
        )
        val withoutNumber = sender.send(NOTIFICATION)

        // then
        assertEquals(NotificationDeliveryResult.Sent(null), withNumber)
        assertEquals(NotificationDeliveryResult.Sent(null), withoutNumber)
        server.verify()
    }

    @Test
    @DisplayName("Slack 메시지에서 사용자 입력 제어 문자를 이스케이프한다")
    fun `제어 문자를 슬랙 문법으로 처리하지 않는다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val sender = SlackNotificationSender(
            builder.build(),
            SlackProperties(advertisementInquiry = WEBHOOK_URL),
            ObjectMapper(),
        )
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.text").value("📢 광고 계약 신청 - 샘플 &amp; 기업"))
            .andExpect(jsonPath("$.blocks[1].fields[2].text").value("*공고명*\n&lt;!channel&gt; 지원자 모집"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        val result = sender.send(NOTIFICATION.copy(payloadJson = ObjectMapper().writeValueAsString(PARAMETERS + mapOf(
            "companyName" to "샘플 & 기업",
            "recruitmentTitle" to "<!channel> 지원자 모집",
        ))))

        // then
        assertEquals(NotificationDeliveryResult.Sent(null), result)
        server.verify()
    }

    @Test
    @DisplayName("웹훅 설정이 없으면 발송 실패 결과를 기록한다")
    fun `웹훅이 비어 있으면 설정 오류로 분류한다`() {
        // given
        val sender = SlackNotificationSender(RestClient.builder().build(), SlackProperties(), ObjectMapper())

        // when
        val result = sender.send(NOTIFICATION)

        // then
        assertEquals(
            NotificationDeliveryResult.Failed(
                "SLACK_WEBHOOK_NOT_CONFIGURED",
                com.ogonggo.core.notification.domain.NotificationFailureCategory.CHANNEL_NOT_CONFIGURED,
            ),
            result,
        )
    }

    private companion object {
        const val WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/xxxx"
        const val ADMIN_BASE_URL = "https://admin.example.com"
        val PARAMETERS = mapOf(
            "contractId" to "1001",
            "promotionTarget" to "채용 공고",
            "companyName" to "샘플기업",
            "recruitmentTitle" to "백엔드 인턴 채용",
            "recruitmentDeadline" to "2026-12-31",
            "purpose" to "적합한 지원자 확보",
            "categories" to "배너 · 인스타그램 1",
            "managerName" to "김담당",
            "email" to "marketing@example.com",
            "phoneNumber" to "02-1234-5678",
        )
        val NOTIFICATION = NotificationMessageDto(
            notificationId = 4,
            channel = NotificationChannel.SLACK,
            templateCode = "advertisement_contract_submitted",
            recipientAddress = "advertisement-inquiry",
            payloadJson = ObjectMapper().writeValueAsString(PARAMETERS),
            deduplicationKey = "advertisement-contract:1001:slack",
            processingAttemptId = "attempt-4",
        )
    }
}
