package com.ogonggo.userapi.banner.business

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.implement.BannerReader
import com.ogonggo.userapi.banner.implement.PublicBannerCache
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class UserBannerService(
    private val bannerReader: BannerReader,
    private val publicBannerCache: PublicBannerCache,
    private val clock: Clock,
) {

    /** 그 자리에 지금 노출 중인 배너를 순서대로 준다. 자리마다 잠깐 캐시하므로 관리자 변경은 조금 늦게 보인다. */
    fun getBanners(placement: BannerPlacement): List<UserBannerResult> =
        publicBannerCache.get(placement) {
            bannerReader.readLive(placement, LocalDateTime.now(clock)).map(UserBannerResult::from)
        }
}
