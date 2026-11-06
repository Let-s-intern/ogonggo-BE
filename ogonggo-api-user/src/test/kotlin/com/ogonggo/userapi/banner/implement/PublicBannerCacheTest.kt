package com.ogonggo.userapi.banner.implement

import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.userapi.banner.business.UserBannerResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class PublicBannerCacheTest {

    @Test
    fun `자리마다 30초 동안은 다시 읽지 않고 그 뒤에는 다시 읽는다`() {
        // given
        val clock = MutableClock(Instant.parse("2026-11-06T03:00:00Z"))
        val cache = PublicBannerCache(clock)
        var loads = 0
        val load = { loads++; listOf(banner(loads.toLong())) }

        // when
        val first = cache.get(BannerPlacement.HOME_TOP, load)
        clock.now = clock.now.plusSeconds(29)
        val cached = cache.get(BannerPlacement.HOME_TOP, load)
        cache.get(BannerPlacement.HOME_BOTTOM, load)
        clock.now = clock.now.plusSeconds(1)
        val reloaded = cache.get(BannerPlacement.HOME_TOP, load)

        // then
        assertEquals(first, cached)
        assertEquals(3, loads)
        assertEquals(3L, reloaded.single().id)
    }

    private fun banner(id: Long) = UserBannerResult(
        id = id,
        placement = BannerPlacement.HOME_TOP,
        pcImageUrl = "/banners/jobs-pc.svg",
        mobileImageUrl = null,
        linkUrl = "/calendar",
        openInNewTab = false,
        altText = "새 공고",
        showAdLabel = false,
    )

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }
}
