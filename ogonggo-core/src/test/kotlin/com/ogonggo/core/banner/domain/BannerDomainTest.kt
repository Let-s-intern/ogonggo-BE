package com.ogonggo.core.banner.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class BannerDomainTest {

    @Test
    fun `켜져 있고 시작은 포함 끝은 미포함인 기간 안일 때만 노출 중이다`() {
        // given
        val banner = Banner(BannerPlacement.HOME_TOP, settings(startAt = NOW, endAt = NOW.plusDays(1)), displayOrder = 1)

        // when & then
        assertFalse(banner.isLive(NOW.minusSeconds(1)))
        assertTrue(banner.isLive(NOW))
        assertFalse(banner.isLive(NOW.plusDays(1)))
        assertTrue(Banner(BannerPlacement.HOME_TOP, settings(), displayOrder = 1).isLive(NOW))
        assertFalse(Banner(BannerPlacement.HOME_TOP, settings(enabled = false), displayOrder = 1).isLive(NOW))
    }

    @Test
    fun `지운 배너는 노출되지 않고 다시 지워도 처음 삭제 시각을 지킨다`() {
        // given
        val banner = Banner(BannerPlacement.HOME_TOP, settings(), displayOrder = 1)

        // when
        banner.delete(NOW)
        banner.delete(NOW.plusDays(1))

        // then
        assertEquals(NOW, banner.deletedAt)
        assertFalse(banner.isLive(NOW))
    }

    @Test
    fun `상세 사이드 자리는 모바일 이미지가 있어야 하고 목록 자리는 비워도 된다`() {
        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            Banner(BannerPlacement.JOB_DETAIL_SIDE, settings(mobileImageUrl = null), displayOrder = 1)
        }
        val banner = Banner(BannerPlacement.BOOTCAMP_DETAIL_SIDE, settings(), displayOrder = 1)
        assertThrows(IllegalArgumentException::class.java) { banner.change(settings(mobileImageUrl = null)) }
        assertNull(Banner(BannerPlacement.BOOTCAMP_TOP, settings(mobileImageUrl = null), displayOrder = 1).mobileImageUrl)
    }

    @Test
    fun `광고 표시가 켜진 배너는 광고주가 있어야 하고 기간은 시작이 끝보다 앞서야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { settings(showAdLabel = true, advertiserId = null) }
        assertThrows(IllegalArgumentException::class.java) { settings(startAt = NOW, endAt = NOW) }
    }

    @Test
    fun `이미지는 https 주소나 웹 기본 소재 경로만, 링크는 http(s) 주소나 사이트 안 경로만 받는다`() {
        assertTrue(BannerSettings.isImageUrl("https://cdn.example.com/ogonggo/banner/a.png"))
        assertTrue(BannerSettings.isImageUrl("/banners/jobs-pc.svg"))
        assertFalse(BannerSettings.isImageUrl("http://cdn.example.com/a.png"))
        assertFalse(BannerSettings.isImageUrl("/images/a.png"))
        assertFalse(BannerSettings.isImageUrl("/banners/../secret"))
        assertTrue(BannerSettings.isLinkUrl("/calendar"))
        assertTrue(BannerSettings.isLinkUrl("http://example.com/x"))
        assertFalse(BannerSettings.isLinkUrl("//evil.example.com"))
        assertFalse(BannerSettings.isLinkUrl("javascript:alert(1)"))
    }

    @Test
    fun `이름과 대체 텍스트는 앞뒤 공백을 뺀다`() {
        // when
        val settings = settings(name = "  홈 배너  ")

        // then
        assertEquals("홈 배너", settings.name)
    }

    companion object {
        private val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 6, 12, 0)

        internal fun settings(
            name: String = "홈 배너",
            mobileImageUrl: String? = "/banners/jobs-mo.svg",
            showAdLabel: Boolean = false,
            advertiserId: Long? = null,
            startAt: LocalDateTime? = null,
            endAt: LocalDateTime? = null,
            enabled: Boolean = true,
        ) = BannerSettings(
            name = name,
            pcImageUrl = "/banners/jobs-pc.svg",
            mobileImageUrl = mobileImageUrl,
            linkUrl = "/calendar",
            openInNewTab = false,
            altText = "매일 아침 올라오는 새 공고",
            showAdLabel = showAdLabel,
            startAt = startAt,
            endAt = endAt,
            enabled = enabled,
            advertiserId = advertiserId,
            contractId = null,
        )
    }
}
