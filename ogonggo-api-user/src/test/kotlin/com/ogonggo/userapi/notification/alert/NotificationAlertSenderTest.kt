package com.ogonggo.userapi.notification.alert

import com.ogonggo.userapi.slack.SlackProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

internal class NotificationAlertSenderTest {
    private val builder = RestClient.builder()
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val clock = MutableAlertClock()
    private val registry = SimpleMeterRegistry()
    private val properties = NotificationAlertProperties(enabled = true, environment = "test")
    private val sender = sender(properties)

    @Test
    @DisplayName("운영 경고는 DB 없이 Slack 웹훅에 Block Kit으로 전송하고 성공을 계측한다")
    fun `웹훅에서 ok를 받아야 전송 성공이다`() {
        // given
        server.expect(requestTo(WEBHOOK)).andExpect(method(HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("[TEST]")))
            .andExpect(jsonPath("$.blocks[0].type").value("section"))
            .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))
        // when
        val result = sender.send(sampleAlert(NotificationAlertType.DELIVERY_UNRESOLVED))
        // then
        assertEquals(NotificationAlertSendResult.SENT, result)
        assertEquals(1.0, registry.get("ogonggo.notification.alerts").tags("type", "DELIVERY_UNRESOLVED", "outcome", "SENT").counter().count())
        server.verify()
    }

    @Test
    @DisplayName("Slack 전송 실패는 실패 지표로 기록하고 transport 내부에서 재시도하지 않는다")
    fun `실패를 기록하고 반환한다`() {
        // given
        server.expect(requestTo(WEBHOOK)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))
        // when
        val failed = sender.send(sampleAlert(NotificationAlertType.ENQUEUE_FAILURE))
        // then
        assertEquals(NotificationAlertSendResult.FAILED, failed)
        assertEquals(1.0, registry.get("ogonggo.notification.alerts").tags("type", "ENQUEUE_FAILURE", "outcome", "FAILED").counter().count())
        server.verify()
    }

    @Test
    @DisplayName("429 응답의 Retry-After 동안 모든 경고의 HTTP 호출을 멈춘다")
    fun `Slack 전송 제한 시간을 준수한다`() {
        // given
        server.expect(requestTo(WEBHOOK)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "30"))
        server.expect(requestTo(WEBHOOK)).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))
        val alert = sampleAlert(NotificationAlertType.ENQUEUE_FAILURE)
        // when
        val limited = sender.send(alert)
        val deferred = sender.send(sampleAlert(NotificationAlertType.MONITORING_FAILURE))
        clock.advance(Duration.ofSeconds(31))
        val resumed = sender.send(alert)
        // then
        assertEquals(NotificationAlertSendResult.RATE_LIMITED, limited)
        assertEquals(NotificationAlertSendResult.RATE_LIMITED, deferred)
        assertEquals(NotificationAlertSendResult.SENT, resumed)
        server.verify()
    }

    @Test
    @DisplayName("비활성화 또는 빈 웹훅에서는 외부 호출을 하지 않는다")
    fun `명시적으로 활성화하고 수신처를 설정해야 전송한다`() {
        // given
        val disabled = sender(properties.copy(enabled = false))
        val missing = sender(properties, SlackProperties())
        // when
        val first = disabled.send(sampleAlert(NotificationAlertType.CLEANUP_BACKLOG))
        val second = missing.send(sampleAlert(NotificationAlertType.CLEANUP_BACKLOG))
        // then
        assertEquals(NotificationAlertSendResult.DISABLED, first)
        assertEquals(NotificationAlertSendResult.DISABLED, second)
        server.verify()
    }

    @Test
    @DisplayName("Slack 응답이 ok가 아니면 전송 실패로 기록한다")
    fun `성공 응답 본문도 확인한다`() {
        // given
        server.expect(requestTo(WEBHOOK)).andRespond(withSuccess("invalid_payload", MediaType.TEXT_PLAIN))
        // when
        val result = sender.send(sampleAlert(NotificationAlertType.CLEANUP_BACKLOG))
        // then
        assertEquals(NotificationAlertSendResult.FAILED, result)
        server.verify()
    }

    @Test
    @DisplayName("Slack 설정 문자열에는 웹훅 비밀 경로를 노출하지 않는다")
    fun `설정 출력에서 웹훅을 숨긴다`() {
        // given
        val slack = SlackProperties(companyPosting = WEBHOOK, notificationAlerts = WEBHOOK)
        // when
        val text = slack.toString()
        // then
        assertFalse(text.contains(WEBHOOK))
        assertFalse(text.contains("test-secret"))
    }

    private fun sender(
        properties: NotificationAlertProperties,
        slack: SlackProperties = SlackProperties(notificationAlerts = WEBHOOK),
    ) = NotificationAlertSender(builder.build(), slack, properties, NotificationAlertTemplate(properties), registry, clock)

    private companion object {
        const val WEBHOOK = "https://hooks.slack.com/services/test/team/test-secret"
    }
}

private class MutableAlertClock : Clock() {
    private var current = Instant.parse("2026-10-24T03:00:00Z")
    override fun instant(): Instant = current
    override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)
    fun advance(duration: Duration) { current = current.plus(duration) }
}
