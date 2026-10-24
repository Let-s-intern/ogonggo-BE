package com.ogonggo.userapi.notification.alert

import com.ogonggo.userapi.slack.SLACK_REST_CLIENT
import com.ogonggo.userapi.slack.SlackProperties
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit

internal enum class NotificationAlertSendResult { SENT, SUPPRESSED, DISABLED, RATE_LIMITED, FAILED }

/** DB와 사용자 알림 대기열을 거치지 않는 운영 경고 발송기다. 전용 실행기에서 호출한다. */
@Component
internal class NotificationAlertSender(
    @Qualifier(SLACK_REST_CLIENT) private val restClient: RestClient,
    private val slack: SlackProperties,
    private val properties: NotificationAlertProperties,
    private val template: NotificationAlertTemplate,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    private val lastSent = mutableMapOf<AlertKey, Instant>()
    private var lastRequestNanos: Long? = null
    private var rateLimitedUntil: Instant = Instant.MIN

    @Synchronized
    fun send(alert: NotificationAlert): NotificationAlertSendResult {
        if (!properties.enabled || slack.notificationAlerts.isBlank()) return record(alert, NotificationAlertSendResult.DISABLED)
        val uri = runCatching { URI(slack.notificationAlerts) }.getOrNull()
        if (uri?.scheme != "https" || uri.host != "hooks.slack.com" || !uri.path.startsWith("/services/")) {
            log.error("알림 운영 경고 웹훅 설정이 올바르지 않습니다. type={}", alert.type.name)
            return record(alert, NotificationAlertSendResult.FAILED)
        }
        val now = clock.instant()
        lastSent.entries.removeIf { !it.value.plus(properties.cooldown).isAfter(now) }
        // 키는 타입·상태·고정 범위뿐이다. 동적인 알림 ID나 원본 예외를 포함하지 않는다.
        val key = AlertKey(alert.type, alert.status, alert.scope)
        if (alert.status != NotificationAlertStatus.TEST && lastSent[key]?.plus(properties.cooldown)?.isAfter(now) == true) {
            return record(alert, NotificationAlertSendResult.SUPPRESSED)
        }
        if (now.isBefore(rateLimitedUntil)) return record(alert, NotificationAlertSendResult.RATE_LIMITED)

        return try {
            lastRequestNanos?.let { previous ->
                val remaining = properties.minSendInterval.toNanos() - (System.nanoTime() - previous)
                if (remaining > 0) TimeUnit.NANOSECONDS.sleep(remaining)
            }
            lastRequestNanos = System.nanoTime()
            val response = restClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON)
                .body(template.render(alert)).retrieve().body(String::class.java)
            if (response?.trim() != "ok") {
                log.error("알림 운영 경고의 Slack 접수를 확인하지 못했습니다. type={}", alert.type.name)
                record(alert, NotificationAlertSendResult.FAILED)
            } else {
                // 발생·복구가 바뀌면 이전 상태의 cooldown을 해제해 재발을 바로 알릴 수 있게 한다.
                if (alert.status != NotificationAlertStatus.TEST) {
                    lastSent.keys.removeIf { it.type == alert.type && it.scope == alert.scope }
                    lastSent[key] = clock.instant()
                }
                log.info("알림 운영 경고를 Slack에 전달했습니다. type={}, status={}", alert.type.name, alert.status.name)
                record(alert, NotificationAlertSendResult.SENT)
            }
        } catch (exception: RestClientResponseException) {
            if (exception.statusCode.value() == 429) {
                val seconds = exception.responseHeaders?.getFirst("Retry-After")?.toLongOrNull()?.coerceAtLeast(1) ?: 60
                rateLimitedUntil = clock.instant().plusSeconds(seconds)
                log.warn("알림 운영 경고가 Slack 전송 제한에 걸렸습니다. type={}, retryAfterSeconds={}", alert.type.name, seconds)
                record(alert, NotificationAlertSendResult.RATE_LIMITED)
            } else {
                log.error("알림 운영 경고 전송이 거절됐습니다. type={}, httpStatus={}", alert.type.name, exception.statusCode.value())
                record(alert, NotificationAlertSendResult.FAILED)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            record(alert, NotificationAlertSendResult.FAILED)
        } catch (exception: Exception) {
            // HTTP 예외 메시지와 cause에는 웹훅 비밀 경로가 포함될 수 있어 기록하지 않는다.
            log.error("알림 운영 경고 전송에 실패했습니다. type={}, errorType={}", alert.type.name, exception::class.simpleName)
            record(alert, NotificationAlertSendResult.FAILED)
        }
    }

    private fun record(alert: NotificationAlert, result: NotificationAlertSendResult): NotificationAlertSendResult {
        meterRegistry.counter("ogonggo.notification.alerts", "type", alert.type.name, "outcome", result.name).increment()
        return result
    }

    private companion object {
        val log = LoggerFactory.getLogger(NotificationAlertSender::class.java)
    }

    private data class AlertKey(val type: NotificationAlertType, val status: NotificationAlertStatus, val scope: String)
}
