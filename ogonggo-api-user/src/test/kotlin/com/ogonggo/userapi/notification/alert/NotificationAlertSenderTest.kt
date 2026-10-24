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
    @DisplayName("같은 경고는 재전송 간격 안에 억제하고 간격 이후 다시 보낸다")
    fun `성공한 같은 유형과 범위의 경고를 중복 전송하지 않는다`() {
        // given
        repeat(2) { server.expect(requestTo(WEBHOOK)).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN)) }
        val alert = sampleAlert(NotificationAlertType.CLEANUP_BACKLOG).copy(status = NotificationAlertStatus.FIRING)
        // when
        val first = sender.send(alert)
        val duplicate = sender.send(alert)
        clock.advance(Duration.ofMinutes(16))
        val repeated = sender.send(alert)
        // then
        assertEquals(NotificationAlertSendResult.SENT, first)
        assertEquals(NotificationAlertSendResult.SUPPRESSED, duplicate)
        assertEquals(NotificationAlertSendResult.SENT, repeated)
        server.verify()
    }

    @Test
    @DisplayName("복구 후 같은 장애가 재발하면 이전 발생 경고의 재전송 간격에 막히지 않는다")
    fun `발생 복구 재발을 각각 전송한다`() {
        // given
        repeat(3) { server.expect(requestTo(WEBHOOK)).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN)) }
        val alert = sampleAlert(NotificationAlertType.DELIVERY_STALLED).copy(status = NotificationAlertStatus.FIRING)
        // when
        val first = sender.send(alert)
        val recovered = sender.send(alert.copy(status = NotificationAlertStatus.RESOLVED))
        val recurrence = sender.send(alert)
        // then
        assertEquals(listOf(NotificationAlertSendResult.SENT, NotificationAlertSendResult.SENT, NotificationAlertSendResult.SENT), listOf(first, recovered, recurrence))
        server.verify()
    }

    @Test
    @DisplayName("Slack 전송 실패는 cooldown에 기록하지 않아 다음 감지 때 다시 전송할 수 있다")
    fun `실패한 경고를 전송 성공으로 취급하지 않는다`() {
        // given
        server.expect(requestTo(WEBHOOK)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))
        server.expect(requestTo(WEBHOOK)).andRespond(withSuccess("ok", MediaType.TEXT_PLAIN))
        val alert = sampleAlert(NotificationAlertType.ENQUEUE_FAILURE).copy(status = NotificationAlertStatus.FIRING)
        // when
        val failed = sender.send(alert)
        val retried = sender.send(alert)
        // then
        assertEquals(NotificationAlertSendResult.FAILED, failed)
        assertEquals(NotificationAlertSendResult.SENT, retried)
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
