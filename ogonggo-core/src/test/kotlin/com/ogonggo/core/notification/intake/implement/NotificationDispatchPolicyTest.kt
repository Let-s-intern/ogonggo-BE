package com.ogonggo.core.notification.intake.implement

import com.ogonggo.core.notification.domain.NotificationChannel
import com.ogonggo.core.notification.intake.implement.dto.NotificationAppendDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import java.time.LocalDateTime

internal class NotificationDispatchPolicyTest {
    private val now = LocalDateTime.of(2026, 10, 24, 12, 0)

    @ParameterizedTest
    @EnumSource(NotificationChannel::class)
    @DisplayName("설정하지 않은 채널은 예정 시각부터 기본 8분 동안 발송할 수 있다")
    fun `모든 채널의 초기 기본 창은 8분이다`(channel: NotificationChannel) {
        // given
        val policy = NotificationDispatchPolicy(NotificationDispatchProperties())
        val scheduledAt = now.plusDays(1)

        // when
        val expiresAt = policy.expiresAt(notification(channel, scheduledAt), now)

        // then
        assertEquals(scheduledAt.plusMinutes(8), expiresAt)
    }

    @Test
    @DisplayName("늦게 적재한 알림은 적재 시각부터 채널별 발송 창을 적용한다")
    fun `예정 시각이 지난 알림은 적재 시각을 기준으로 기한을 정한다`() {
        // given
        val policy = NotificationDispatchPolicy(NotificationDispatchProperties(
            windows = mapOf(NotificationChannel.FCM to Duration.ofMinutes(30)),
        ))

        // when
        val expiresAt = policy.expiresAt(notification(NotificationChannel.FCM, now.minusHours(1)), now)

        // then
        assertEquals(now.plusMinutes(30), expiresAt)
    }

    @Test
    @DisplayName("명시한 발송 기한은 채널 기본값보다 우선하며 이미 지난 기한도 연장하지 않는다")
    fun `명시한 기한은 그대로 보존한다`() {
        // given
        val policy = NotificationDispatchPolicy(NotificationDispatchProperties())
        val expiresAt = now.minusSeconds(1)
        val notification = notification(NotificationChannel.FCM, now).copy(dispatchExpiresAt = expiresAt)

        // when
        val resolved = policy.expiresAt(notification, now)

        // then
        assertEquals(expiresAt, resolved)
    }

    @Test
    @DisplayName("설정의 채널별 발송 창이 기본 창보다 우선하고 누락한 채널은 기본 창을 사용한다")
    fun `공통 설정으로 채널별 기한을 계산한다`() {
        // given
        val runner = ApplicationContextRunner()
            .withUserConfiguration(NotificationDispatchPolicy::class.java)
            .withPropertyValues(
                "ogonggo.notification.dispatch.default-window=10m",
                "ogonggo.notification.dispatch.windows.email=30m",
                "ogonggo.notification.dispatch.windows.fcm=2m",
            )

        // when
        runner.run { context ->
            val policy = context.getBean(NotificationDispatchPolicy::class.java)

            // then
            assertEquals(now.plusMinutes(10), policy.expiresAt(notification(NotificationChannel.KAKAO, now), now))
            assertEquals(now.plusMinutes(30), policy.expiresAt(notification(NotificationChannel.EMAIL, now), now))
            assertEquals(now.plusMinutes(2), policy.expiresAt(notification(NotificationChannel.FCM, now), now))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "ogonggo.notification.dispatch.default-window=0s",
        "ogonggo.notification.dispatch.default-window=-1m",
        "ogonggo.notification.dispatch.windows.fcm=0s",
        "ogonggo.notification.dispatch.windows.email=-1m",
    ])
    @DisplayName("0 이하의 기본 또는 채널별 발송 창은 기동 시 거부한다")
    fun `양수가 아닌 발송 창은 설정할 수 없다`(property: String) {
        // given
        val runner = ApplicationContextRunner()
            .withUserConfiguration(NotificationDispatchPolicy::class.java)
            .withPropertyValues(property)

        // when
        runner.run { context ->
            // then
            assertNotNull(context.startupFailure)
        }
    }

    private fun notification(channel: NotificationChannel, scheduledAt: LocalDateTime) = NotificationAppendDto(
        deduplicationKey = "test:$channel",
        channel = channel,
        templateCode = "test",
        recipientAddress = "test-recipient",
        payloadJson = "{}",
        scheduledAt = scheduledAt,
    )
}
