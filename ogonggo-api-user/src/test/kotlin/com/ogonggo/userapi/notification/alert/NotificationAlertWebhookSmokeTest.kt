package com.ogonggo.userapi.notification.alert

import com.ogonggo.userapi.config.UserSlackConfiguration
import com.ogonggo.userapi.slack.SlackProperties
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

/** 사용자 승인 후 명시적으로 실행하는 실제 웹훅 검사다. 일반 테스트는 외부에 전송하지 않는다. */
@EnabledIfEnvironmentVariable(named = "OGONGGO_NOTIFICATION_ALERT_SMOKE", matches = "true")
internal class NotificationAlertWebhookSmokeTest {
    @Test
    @DisplayName("실제 Slack 웹훅으로 TEST 표시한 8개 경고 템플릿을 검증한다")
    fun `8개 경고 유형을 실제 웹훅에 테스트 전송한다`() {
        // given
        val webhook = requireNotNull(System.getenv("OGONGGO_NOTIFICATION_ALERT_WEBHOOK"))
        val slack = SlackProperties(notificationAlerts = webhook)
        val properties = NotificationAlertProperties(enabled = true, environment = "local-test", instanceId = "codex-webhook-smoke")
        val clock = Clock.system(ZoneId.of("Asia/Seoul"))
        val sender = NotificationAlertSender(
            UserSlackConfiguration().slackRestClient(slack), slack, properties,
            NotificationAlertTemplate(properties), SimpleMeterRegistry(), clock,
        )
        NotificationAlertType.entries.forEach { type ->
            // when
            val result = sender.send(sampleAlert(type).copy(observedAt = LocalDateTime.now(clock)))
            // then
            assertEquals(NotificationAlertSendResult.SENT, result, "${type.name} 전송 결과")
            println("Slack TEST 접수 확인: ${type.code} ${type.name}")
        }
    }
}
