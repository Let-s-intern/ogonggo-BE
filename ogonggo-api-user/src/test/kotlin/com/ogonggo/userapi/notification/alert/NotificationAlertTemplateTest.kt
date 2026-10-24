package com.ogonggo.userapi.notification.alert

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime

internal class NotificationAlertTemplateTest {
    private val template = NotificationAlertTemplate(NotificationAlertProperties(environment = "test", instanceId = "instance-1"))

    @ParameterizedTest
    @EnumSource(NotificationAlertType::class)
    @DisplayName("8개 경고 샘플은 테스트 표시와 관측값·영향·확인할 항목을 제공한다")
    fun `모든 경고 유형에 독립적인 테스트 템플릿이 있다`(type: NotificationAlertType) {
        // given
        val alert = sampleAlert(type)
        // when
        val message = template.render(alert)
        val texts = message.blocks.flatMap { listOfNotNull(it.text?.text) + it.fields.orEmpty().map { field -> field.text } }
        // then
        assertTrue(message.text.startsWith("[TEST]"))
        assertTrue(message.text.contains(type.title))
        assertTrue(message.text.contains(type.code))
        assertTrue(texts.any { it.contains("실제 장애나 운영 측정값이 아닙니다") })
        assertTrue(texts.any { it.contains(type.impact) })
        assertTrue(texts.any { it.contains(type.action) })
        assertTrue(texts.any { it.contains("2026-10-24 12:00:00") })
        assertTrue(message.blocks.all { it.text == null || it.fields == null })
    }

    @Test
    @DisplayName("관측값과 환경의 Slack 멘션·링크 제어 문자를 이스케이프한다")
    fun `외부 값이 Slack 멘션으로 해석되지 않는다`() {
        // given
        val template = NotificationAlertTemplate(NotificationAlertProperties(environment = "<!channel>"))
        val alert = sampleAlert(NotificationAlertType.MONITORING_FAILURE).copy(measurements = mapOf("원인" to "<@U123> & failure"))
        // when
        val message = template.render(alert)
        // then
        assertFalse(message.text.contains("<!channel>"))
        assertTrue(message.text.contains("&lt;!channel&gt;"))
        assertTrue(message.blocks.flatMap { it.fields.orEmpty() }.any { it.text.contains("&lt;@U123&gt; &amp; failure") })
    }

    @Test
    @DisplayName("이스케이프로 길이가 늘어나는 관측값도 Slack 필드 제한 안에 들어간다")
    fun `긴 관측값을 안전한 길이로 자른다`() {
        // given
        val alert = sampleAlert(NotificationAlertType.MONITORING_FAILURE)
            .copy(measurements = mapOf("&".repeat(100) to "&".repeat(1_000)))
        // when
        val message = template.render(alert)
        // then
        assertTrue(message.blocks.flatMap { it.fields.orEmpty() }.all { it.text.length <= 2_000 })
    }

    @Test
    @DisplayName("복구 메시지는 정상 상태로 표시하고 기존 장애 문구를 현재 상황으로 단정하지 않는다")
    fun `복구 메시지는 발생 메시지와 구분한다`() {
        // given
        val alert = sampleAlert(NotificationAlertType.DELIVERY_UNRESOLVED).copy(status = NotificationAlertStatus.RESOLVED)
        // when
        val message = template.render(alert)
        // then
        assertTrue(message.text.startsWith("[복구][정상]"))
        assertEquals("감지 조건이 해소되었습니다. 아래 관측값과 후속 처리를 확인하세요.", message.blocks[1].text?.text)
    }
}

internal fun sampleAlert(type: NotificationAlertType) = NotificationAlert(
    type = type,
    status = NotificationAlertStatus.TEST,
    observedAt = LocalDateTime.of(2026, 10, 24, 12, 0),
    scope = when (type) {
        NotificationAlertType.CLEANUP_BACKLOG, NotificationAlertType.CLEANUP_EXECUTION_FAILURE -> "notificationCleanup"
        NotificationAlertType.ENQUEUE_FAILURE -> "signup"
        NotificationAlertType.MONITORING_FAILURE -> "collector"
        else -> "notificationDelivery"
    },
    measurements = when (type) {
        NotificationAlertType.CLEANUP_BACKLOG -> mapOf("삭제 대상 잔량 (샘플)" to "12,000건", "실행 예산 소진 (샘플)" to "true")
        NotificationAlertType.CLEANUP_EXECUTION_FAILURE -> mapOf("예정 시각 (샘플)" to "03:30 Asia/Seoul", "연속 실패 (샘플)" to "2회")
        NotificationAlertType.LOCK_EXPIRY_RISK -> mapOf("실행 중 시간 (샘플)" to "65초", "잠금 상한" to "90초")
        NotificationAlertType.DELIVERY_STALLED -> mapOf("발송 가능 대기 (샘플)" to "120건", "진행 없음 (샘플)" to "90초")
        NotificationAlertType.PENDING_EXPIRED -> mapOf("새 기한 초과 (샘플)" to "5건", "채널 (샘플)" to "FCM")
        NotificationAlertType.DELIVERY_UNRESOLVED -> mapOf("UNRESOLVED 증가 (샘플)" to "1건", "오류 분류 (샘플)" to "DB_COMMIT_FAILED")
        NotificationAlertType.ENQUEUE_FAILURE -> mapOf("적재 실패 (샘플)" to "1건", "사유 (샘플)" to "EXECUTOR_REJECTED")
        NotificationAlertType.MONITORING_FAILURE -> mapOf("표본 수집 실패 (샘플)" to "3회", "수집 대상 (샘플)" to "user-api")
    },
)
