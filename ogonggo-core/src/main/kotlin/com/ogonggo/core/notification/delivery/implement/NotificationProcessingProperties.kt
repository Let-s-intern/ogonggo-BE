package com.ogonggo.core.notification.delivery.implement

import com.ogonggo.core.notification.domain.NotificationProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

enum class NotificationStaleAction {
    RETRY_WITH_IDEMPOTENCY,
    MARK_UNKNOWN,
}

data class NotificationProviderProcessingSettings(
    val staleTimeout: Duration = Duration.ofMinutes(8),
    val staleAction: NotificationStaleAction = NotificationStaleAction.MARK_UNKNOWN,
    val idempotencyWindow: Duration? = null,
    val maxRecoveryRetries: Int = 0,
) {
    init {
        require(!staleTimeout.isZero && !staleTimeout.isNegative) { "provider별 stale timeout은 양수여야 합니다." }
        require(idempotencyWindow == null || (!idempotencyWindow.isZero && !idempotencyWindow.isNegative)) {
            "provider 멱등성 창은 양수여야 합니다."
        }
        require(maxRecoveryRetries >= 0) { "stale 복구 재시도 횟수는 0 이상이어야 합니다." }
        require(
            staleAction != NotificationStaleAction.RETRY_WITH_IDEMPOTENCY ||
                (idempotencyWindow != null && maxRecoveryRetries > 0),
        ) { "멱등성 기반 stale 재시도에는 멱등성 창과 재시도 횟수가 필요합니다." }
    }
}

@ConfigurationProperties(prefix = "ogonggo.notification.processing")
data class NotificationProcessingProperties(
    val nhnAlimTalk: NotificationProviderProcessingSettings = NotificationProviderProcessingSettings(
        staleTimeout = Duration.ofMinutes(8),
        staleAction = NotificationStaleAction.RETRY_WITH_IDEMPOTENCY,
        idempotencyWindow = Duration.ofMinutes(10),
        maxRecoveryRetries = 1,
    ),
    val emailSmtp: NotificationProviderProcessingSettings = NotificationProviderProcessingSettings(
        staleTimeout = Duration.ofMinutes(8),
    ),
    val firebaseFcm: NotificationProviderProcessingSettings = NotificationProviderProcessingSettings(
        staleTimeout = Duration.ofMinutes(8),
    ),
)
