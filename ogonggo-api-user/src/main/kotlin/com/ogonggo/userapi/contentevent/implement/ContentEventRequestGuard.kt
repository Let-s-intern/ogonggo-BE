package com.ogonggo.userapi.contentevent.implement

import org.springframework.stereotype.Component
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 콘텐츠 이벤트 요청을 저장해도 되는지 본다. 막힌 요청은 오류 없이 저장만 하지 않는다.
 *
 * - 알려진 봇·크롤러 User-Agent와 User-Agent가 없는 요청은 사람이 본 것이 아니므로 받지 않는다.
 * - 같은 방문자와 같은 IP의 요청 수를 1분 단위로 세어 상한을 넘으면 받지 않는다.
 *
 * 카운터는 이 인스턴스의 메모리에 있다. 여러 대로 돌면 대마다 따로 세지만 남용을 줄이는 데는 충분하다고 봤다
 * (2026-11-19, LC-3646). 실제 남용이 보이면 공용 저장소로 옮긴다.
 */
@Component
class ContentEventRequestGuard(
    private val clock: Clock,
) {
    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    @Volatile
    private var currentWindow = Long.MIN_VALUE

    fun isAllowed(visitorId: String, clientIp: String?, userAgent: String?): Boolean {
        if (isBot(userAgent)) return false
        val window = clock.millis() / WINDOW_MILLIS
        resetIfNewWindow(window)
        val visitorCount = counters.computeIfAbsent("v:$visitorId") { AtomicInteger() }.incrementAndGet()
        val ipCount = clientIp?.let { counters.computeIfAbsent("i:$it") { AtomicInteger() }.incrementAndGet() } ?: 0
        return visitorCount <= MAX_REQUESTS_PER_VISITOR && ipCount <= MAX_REQUESTS_PER_IP
    }

    /** 1분이 지나면 지난 창의 카운터를 모두 버린다. 메모리가 방문자 수만큼 계속 늘지 않게 한다. */
    private fun resetIfNewWindow(window: Long) {
        if (window == currentWindow) return
        synchronized(this) {
            if (window != currentWindow) {
                counters.clear()
                currentWindow = window
            }
        }
    }

    private fun isBot(userAgent: String?): Boolean {
        if (userAgent.isNullOrBlank()) return true
        val lowered = userAgent.lowercase()
        if (BOT_MARKERS.any { lowered.contains(it) }) return true
        return BOT_NAME.containsMatchIn(lowered) && DEVICE_NAMES.none { lowered.contains(it) }
    }

    companion object {
        const val MAX_REQUESTS_PER_VISITOR = 60
        const val MAX_REQUESTS_PER_IP = 600
        private const val WINDOW_MILLIS = 60_000L

        /** 검색엔진·미리보기·자동화 도구가 User-Agent에 넣는 문자열이다. */
        private val BOT_MARKERS = listOf(
            "crawler", "spider", "slurp", "headless", "lighthouse", "facebookexternalhit", "kakaotalk-scrap",
            "yeti", "daumoa", "curl", "wget", "python-requests", "httpclient", "okhttp", "go-http-client", "java/", "axios",
            "node-fetch", "postman",
        )

        /**
         * `Googlebot`·`bingbot`·`AhrefsBot`처럼 이름이 `bot`으로 끝나는 단어다. 단어 경계로 맞춰 `CUBOT_X30`처럼 단말 이름 안에 붙은 `bot`은 거른다.
         * `daum`은 다음 앱 내장 브라우저(`DaumApps`)에도 있어 크롤러 이름 `daumoa`만 본다.
         */
        private val BOT_NAME = Regex("""\b[a-z0-9]*bot\b""")

        /** 이름이 `bot`으로 끝나지만 사람이 쓰는 단말이다. */
        private val DEVICE_NAMES = listOf("cubot")
    }
}
