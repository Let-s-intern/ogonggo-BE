package com.ogonggo.userapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposurePageDto
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AdvertisementExposureServiceTest {

    @Test
    fun `고정 시각과 배너 자리로 공개 노출을 조회해 공개 필드만 반환한다`() {
        // given
        val reader = Mockito.mock(AdvertisementExposureReader::class.java)
        val exposure = Mockito.mock(AdvertisementExposure::class.java)
        Mockito.`when`(exposure.id).thenReturn(EXPOSURE_ID)
        Mockito.`when`(exposure.name).thenReturn("샘플기업 인턴 채용")
        Mockito.`when`(exposure.imageUrl).thenReturn("https://example.com/banner.png")
        Mockito.`when`(exposure.linkUrl).thenReturn("/jobs/5501")
        Mockito.`when`(exposure.adLabel).thenReturn(true)
        Mockito.`when`(reader.readShownPage(POSITION, NOW, 0, 10)).thenReturn(
            AdvertisementExposurePageDto(listOf(exposure), page = 0, size = 10, totalElements = 1, totalPages = 1),
        )
        val service = AdvertisementExposureService(reader, CLOCK)

        // when
        val results = service.getShown(POSITION, 0, 10)

        // then
        assertEquals(
            AdvertisementExposurePageResult(
                items = listOf(
                AdvertisementExposureResult(
                    id = EXPOSURE_ID,
                    name = "샘플기업 인턴 채용",
                    imageUrl = "https://example.com/banner.png",
                    linkUrl = "/jobs/5501",
                    adLabel = true,
                ),
                ),
                page = 0,
                size = 10,
                totalElements = 1,
                totalPages = 1,
            ),
            results,
        )
        Mockito.verify(reader).readShownPage(POSITION, NOW, 0, 10)
    }

    private companion object {
        const val EXPOSURE_ID = 501L
        val POSITION = AdvertisementProductPosition.JOB_HOME_TOP_BANNER
        val NOW = LocalDateTime.of(2026, 11, 7, 12, 0)
        val CLOCK = Clock.fixed(Instant.parse("2026-11-07T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    }
}
