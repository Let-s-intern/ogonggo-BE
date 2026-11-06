package com.ogonggo.userapi.banner.implement

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.userapi.banner.business.UserBannerResult
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 공개 배너 조회 결과를 자리마다 [TTL] 동안 이 프로세스 메모리에 둔다. 홈·상세 서버 렌더마다 불리므로 DB를 매번 읽지 않는다.
 *
 * 배너를 바꾸는 쓰기는 관리자 API(다른 프로세스)에서 일어나 이 캐시를 비울 수 없다. 그래서 쓰기와 기간 경계(시작·끝 시각)는
 * 최대 [TTL]만큼 늦게 반영된다. 사용자 API 컨테이너가 여럿이면 각자 캐시를 가진다.
 */
@Component
class PublicBannerCache(
    private val clock: Clock,
) {
    private val entries = ConcurrentHashMap<BannerPlacement, Entry>()

    /** 만료 전 결과가 있으면 그것을, 없으면 [load]로 읽어 두고 돌려준다. 같은 자리를 동시에 읽으면 둘 다 읽을 수 있다. */
    fun get(placement: BannerPlacement, load: () -> List<UserBannerResult>): List<UserBannerResult> {
        val now = clock.instant()
        entries[placement]?.takeIf { now.isBefore(it.expiresAt) }?.let { return it.banners }
        return load().also { entries[placement] = Entry(it, now.plus(TTL)) }
    }

    private data class Entry(
        val banners: List<UserBannerResult>,
        val expiresAt: Instant,
    )

    companion object {
        val TTL: Duration = Duration.ofSeconds(30)
    }
}
