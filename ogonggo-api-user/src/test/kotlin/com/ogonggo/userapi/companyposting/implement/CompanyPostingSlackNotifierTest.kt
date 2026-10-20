package com.ogonggo.userapi.companyposting.implement

import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.domain.JobRole
import com.ogonggo.userapi.companyposting.business.CompanyPostingRegisteredEvent
import com.ogonggo.userapi.companyposting.business.CompanyPostingType
import com.ogonggo.userapi.slack.SlackProperties
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

class CompanyPostingSlackNotifierTest {

    @Test
    fun `종류와 기업명과 제목을 알림 제목에 담아 웹훅으로 보낸다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = CompanyPostingSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.text").value("🆕 비즈니스 채용공고 등록 - 오공고 · 백엔드 개발자"))
            .andExpect(jsonPath("$.blocks[0].fields[1].text").value("*ID*\n3"))
            .andExpect(jsonPath("$.blocks[0].fields[3].text").value("*등록 계정*\ncompany@example.com"))
            // `>`는 이스케이프되어 나가고 슬랙 화면에는 `>`로 보인다.
            .andExpect(jsonPath("$.blocks[0].fields[4].text").value("*직무*\nIT·개발 &gt; 백엔드"))
            .andExpect(jsonPath("$.blocks[1].text.text").value("*제목*\n백엔드 개발자"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(EVENT)

        server.verify()
    }

    @Test
    fun `부트캠프는 직무 대신 모집 마감을 보낸다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = CompanyPostingSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.text").value("🆕 비즈니스 부트캠프 등록 - 오공고 · 백엔드 개발자"))
            .andExpect(jsonPath("$.blocks[0].fields[4].text").value("*모집 마감*\n2026-11-30 23:59"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(
            EVENT.copy(
                type = CompanyPostingType.BOOTCAMP,
                jobField = null,
                jobRole = null,
                recruitmentEndAt = LocalDateTime.of(2026, 11, 30, 23, 59),
            ),
        )

        server.verify()
    }

    @Test
    fun `기업회원이 넣은 슬랙 제어 문자를 그대로 보내지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = CompanyPostingSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL))
            .andExpect(jsonPath("$.text").value("🆕 비즈니스 부트캠프 등록 - A&amp;B · &lt;!channel&gt;"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))

        notifier.notify(
            EVENT.copy(type = CompanyPostingType.BOOTCAMP, companyName = "A&B", title = "<!channel>"),
        )

        server.verify()
    }

    @Test
    fun `웹훅이 실패해도 예외를 던지지 않는다`() {
        // 공고는 이미 커밋됐다. 알림 실패가 등록 결과를 바꾸면 안 된다.
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = CompanyPostingSlackNotifier(builder.build(), properties(WEBHOOK_URL))

        server.expect(requestTo(WEBHOOK_URL)).andRespond(withServerError())

        assertDoesNotThrow { notifier.notify(EVENT) }
        server.verify()
    }

    @Test
    fun `웹훅 주소가 비어 있으면 보내지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val notifier = CompanyPostingSlackNotifier(builder.build(), properties(""))

        notifier.notify(EVENT)

        server.verify()
    }

    private fun properties(companyPosting: String): SlackProperties =
        SlackProperties(companyPosting = companyPosting)

    companion object {
        private const val WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/xxxx"
        private val EVENT = CompanyPostingRegisteredEvent(
            type = CompanyPostingType.JOB,
            postingId = 3L,
            companyName = "오공고",
            title = "백엔드 개발자",
            accountEmail = "company@example.com",
            jobField = JobField.IT_DEVELOPMENT,
            jobRole = JobRole.IT_BACKEND,
        )
    }
}
