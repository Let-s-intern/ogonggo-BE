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
            .andExpect(jsonPath("$.blocks[1].fields[9]").doesNotExist())
            .andExpect(jsonPath("$.blocks[2].text.text").value("<https://admin.example.com/business/ad-applications/1001|어드민에서 신청 처리>"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        val result = sender.send(NOTIFICATION)

        // then
        assertEquals(NotificationDeliveryResult.Sent(null), result)
        server.verify()
    }

    @Test
    @DisplayName("광고 계약 Slack 알림은 예정 예산을 적었을 때만 함께 보낸다")
    fun `예정 예산을 적은 신청은 예산 라벨을 포함한다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val sender = SlackNotificationSender(
            builder.build(),
            SlackProperties(advertisementInquiry = WEBHOOK_URL, adminBaseUrl = ADMIN_BASE_URL),
            ObjectMapper(),
        )
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.blocks[1].fields[9].text").value("*예정 예산*\n1,500,000원"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))
        val notification = NOTIFICATION.copy(
            payloadJson = ObjectMapper().writeValueAsString(PARAMETERS + ("budgetAmount" to "1,500,000원")),
        )

        // when
        val result = sender.send(notification)

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
    @DisplayName("광고 계약 Slack 알림은 폼 답변이 있을 때만 항목 아래 어드민 링크 위에 답변 블록을 보낸다")
    fun `폼 답변이 있을 때만 답변 블록을 보낸다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val sender = SlackNotificationSender(
            builder.build(),
            SlackProperties(advertisementInquiry = WEBHOOK_URL, adminBaseUrl = ADMIN_BASE_URL),
            ObjectMapper(),
        )
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.blocks.length()").value(4))
            .andExpect(jsonPath("$.blocks[2].text.text").value("*신청서 답변*\n희망 상품: 메인 배너 &amp; 카카오톡 CRM\n희망 기간: 2주"))
            .andExpect(jsonPath("$.blocks[3].text.text").value("<https://admin.example.com/business/ad-applications/1001|어드민에서 신청 처리>"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.blocks.length()").value(3))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        val withAnswers = sender.send(
            NOTIFICATION.copy(
                payloadJson = ObjectMapper().writeValueAsString(
                    PARAMETERS + ("formAnswers" to "희망 상품: 메인 배너 & 카카오톡 CRM\n희망 기간: 2주"),
                ),
            ),
        )
        val withoutAnswers = sender.send(NOTIFICATION)

        // then
        assertEquals(NotificationDeliveryResult.Sent(null), withAnswers)
        assertEquals(NotificationDeliveryResult.Sent(null), withoutAnswers)
        server.verify()
    }

    @Test
    @DisplayName("이스케이프로 길어진 폼 답변은 Slack section 한도 3000자 안에서 줄 단위로 끊는다")
    fun `폼 답변 블록은 section 한도를 넘지 않는다`() {
        // given
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val sender = SlackNotificationSender(
            builder.build(),
            SlackProperties(advertisementInquiry = WEBHOOK_URL),
            ObjectMapper(),
        )
        val lines = List(30) { "문항$it: " + "&".repeat(80) }.joinToString("\n")
        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.blocks[2].text.text").value(org.hamcrest.Matchers.endsWith("…(이하 생략, 어드민에서 확인)")))
            .andExpect(
                jsonPath("$.blocks[2].text.text").value(
                    org.hamcrest.Matchers.matchesPattern("(?s).{1,3000}"),
                ),
            )
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        // when
        val result = sender.send(NOTIFICATION.copy(payloadJson = ObjectMapper().writeValueAsString(PARAMETERS + ("formAnswers" to lines))))

        // then
        assertEquals(NotificationDeliveryResult.Sent(null), result)
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
