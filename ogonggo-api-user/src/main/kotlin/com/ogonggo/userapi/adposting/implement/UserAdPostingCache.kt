package com.ogonggo.userapi.adposting.implement

import com.ogonggo.core.adposting.domain.AdPostingPlacement
import com.ogonggo.userapi.adposting.business.UserAdPostingResult
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 자리별 공개 광고 공고를 이 프로세스 메모리에 [TTL] 동안 둔다. 홈·공고 상세가 서버 렌더마다 부르기 때문이다.
 *
 * 광고 공고를 바꾸는 관리자 API는 다른 프로세스라 이 캐시를 비울 수 없다. 그래서 바꾼 값은 길어야 [TTL] 뒤에 나간다.
 * 컨테이너가 여럿이면 컨테이너마다 따로 둔다. 만료된 자리를 여러 요청이 동시에 읽으면 각자 다시 읽고 마지막 값이 남는다.
 */
@Component
class UserAdPostingCache(
    private val clock: Clock,
) {
    private val entries = ConcurrentHashMap<AdPostingPlacement, Entry>()

    fun get(placement: AdPostingPlacement, load: () -> List<UserAdPostingResult>): List<UserAdPostingResult> {
        val now = clock.instant()
        entries[placement]?.takeIf { now.isBefore(it.expiresAt) }?.let { return it.items }
        val items = load()
        entries[placement] = Entry(items, now.plus(TTL))
        return items
    }

    private data class Entry(val items: List<UserAdPostingResult>, val expiresAt: Instant)

    companion object {
        val TTL: Duration = Duration.ofSeconds(30)
    }
}
