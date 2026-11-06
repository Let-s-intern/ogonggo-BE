package com.ogonggo.adminapi.banner.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.advertiser.implement.AdvertiserReader
import com.ogonggo.core.advertisercontract.implement.AdvertiserContractReader
import com.ogonggo.core.banner.domain.Banner
import com.ogonggo.core.banner.domain.BannerPlacement
import com.ogonggo.core.banner.implement.BannerAppender
import com.ogonggo.core.banner.implement.BannerManager
import com.ogonggo.core.banner.implement.BannerReader
import com.ogonggo.core.image.implement.ImageAssetReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock

class AdminBannerServiceTest {

    private val bannerReader = Mockito.mock(BannerReader::class.java)
    private val bannerManager = Mockito.mock(BannerManager::class.java)
    private val service = AdminBannerService(
        bannerReader = bannerReader,
        bannerAppender = Mockito.mock(BannerAppender::class.java),
        bannerManager = bannerManager,
        advertiserReader = Mockito.mock(AdvertiserReader::class.java),
        advertiserContractReader = Mockito.mock(AdvertiserContractReader::class.java),
        imageAssetReader = Mockito.mock(ImageAssetReader::class.java),
        clock = Clock.systemDefaultZone(),
    )

    @Test
    fun `순서 바꾸기는 그 자리의 배너 id가 빠지거나 겹치거나 다른 자리 id가 섞이면 아무것도 바꾸지 않고 거절한다`() {
        // given
        val banners = listOf(banner(1), banner(2), banner(3))
        Mockito.`when`(bannerReader.readAllInPlacementForUpdate(BannerPlacement.HOME_TOP)).thenReturn(banners)

        // when & then
        listOf(listOf(1L, 2L), listOf(1L, 2L, 2L), listOf(1L, 2L, 9L), listOf(1L, 2L, 3L, 9L), null).forEach { ids ->
            val exception = assertThrows(AdminConsoleBadRequestException::class.java) {
                service.replaceOrder(BannerPlacement.HOME_TOP, ids)
            }
            assertEquals("bannerIds: 그 자리의 배너 id 를 빠짐없이 한 번씩 보내 주세요.", exception.message)
        }
        Mockito.verifyNoInteractions(bannerManager)
    }

    @Test
    fun `순서 바꾸기는 보낸 순서대로 매긴다`() {
        // given
        val banners = listOf(banner(1), banner(2), banner(3))
        Mockito.`when`(bannerReader.readAllInPlacementForUpdate(BannerPlacement.HOME_TOP)).thenReturn(banners)

        // when
        service.replaceOrder(BannerPlacement.HOME_TOP, listOf(3L, 1L, 2L))

        // then
        Mockito.verify(bannerManager).reorder(listOf(banners[2], banners[0], banners[1]))
    }

    @Test
    fun `자리를 빠뜨리면 자리 목록 전부를 적은 문구로 거절한다`() {
        val exception = assertThrows(AdminConsoleBadRequestException::class.java) { service.replaceOrder(null, listOf(1L)) }
        assertEquals(
            "placement: HOME_TOP, HOME_BOTTOM, JOB_DETAIL_SIDE, BOOTCAMP_TOP, BOOTCAMP_BOTTOM, BOOTCAMP_DETAIL_SIDE 중 하나를 보내 주세요.",
            exception.message,
        )
    }

    private fun banner(id: Long): Banner = Mockito.mock(Banner::class.java).also {
        Mockito.`when`(it.id).thenReturn(id)
    }
}
