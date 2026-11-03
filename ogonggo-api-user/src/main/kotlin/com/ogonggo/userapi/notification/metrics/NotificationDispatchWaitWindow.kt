package com.ogonggo.userapi.notification.metrics

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

/**
 * 발송 작업이 개별 알림(관리자 발송 묶음이 아닌 것)을 꺼낼 때까지 기다린 시간을 최근 구간만큼 메모리에 보관한다.
 * 발송 지연 경보가 최근 1분의 느린 쪽 5%(p95)를 계산할 때 쓴다.
 *
 * 발송 작업은 잠금을 잡은 인스턴스 한 곳에서만 돌기 때문에, 다른 인스턴스의 보관값은 비어 있다.
 * 잠금을 잡은 인스턴스가 바뀌거나 재기동하면 보관값이 초기화된다.
 */
@Component
class NotificationDispatchWaitWindow {
    private val samples = ArrayDeque<Sample>()

    @Synchronized
    fun record(observedAt: Instant, wait: Duration) {
        samples.addLast(Sample(observedAt, wait.toMillis().coerceAtLeast(0)))
        while (samples.size > MAX_SAMPLES) samples.removeFirst()
    }

    /** since 이후에 기록된 대기 시간의 p95와 표본 수. 표본이 없으면 null이다. */
    @Synchronized
    fun p95Since(since: Instant): WaitPercentile? {
        while (samples.isNotEmpty() && samples.first().observedAt.isBefore(since)) samples.removeFirst()
        if (samples.isEmpty()) return null
        val sorted = samples.map { it.waitMillis }.sorted()
        val index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return WaitPercentile(Duration.ofMillis(sorted[index]), sorted.size)
    }

    data class WaitPercentile(val p95: Duration, val samples: Int)

    private data class Sample(val observedAt: Instant, val waitMillis: Long)

    private companion object {
        // 1분 동안 발송 상한을 크게 넘는 표본 수. 메모리 사용을 고정한다.
        const val MAX_SAMPLES = 20_000
    }
}
