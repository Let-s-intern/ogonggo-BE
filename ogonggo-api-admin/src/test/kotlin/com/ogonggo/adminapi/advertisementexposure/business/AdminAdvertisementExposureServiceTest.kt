package com.ogonggo.adminapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureContent
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposurePeriod
import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureAppender
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureManager
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.implement.AdvertisementProductReader
import com.ogonggo.core.error.InvalidValueException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class AdminAdvertisementExposureServiceTest {

    @Test
    @DisplayName("노출 가능한 상품에 배너를 등록하면 생성한 식별자를 반환한다")
    fun `배너 등록 결과로 생성한 노출 ID를 반환한다`() {
        // given
        val productReader = Mockito.mock(AdvertisementProductReader::class.java)
        val exposureAppender = Mockito.mock(AdvertisementExposureAppender::class.java)
        val bannerProduct = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(productReader.readForUpdate(PRODUCT_ID)).thenReturn(bannerProduct)
        Mockito.`when`(exposureAppender.append(Mockito.any() ?: exposureAppendDto())).thenReturn(EXPOSURE_ID)
        val target = service(productReader, exposureAppender)

        // when
        val result = target.createExposure(createCommand())

        // then
        assertEquals(EXPOSURE_ID, result)
    }

    @Test
    @DisplayName("노출을 지원하지 않는 상품에는 배너를 등록할 수 없다")
    fun `외부 채널 상품은 오공고 노출 설정에 등록하지 않는다`() {
        // given
        val productReader = Mockito.mock(AdvertisementProductReader::class.java)
        val instagramProduct = product(AdvertisementProductCategory.INSTAGRAM_1)
        Mockito.`when`(productReader.readForUpdate(PRODUCT_ID)).thenReturn(instagramProduct)
        val target = service(productReader, Mockito.mock(AdvertisementExposureAppender::class.java))

        // when
        val exception = assertThrows(InvalidValueException::class.java) {
            target.createExposure(createCommand())
        }

        // then
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE, exception.errorCode)
    }

    private fun service(
        productReader: AdvertisementProductReader,
        exposureAppender: AdvertisementExposureAppender,
    ) = AdminAdvertisementExposureService(
        Mockito.mock(AdvertisementExposureReader::class.java),
        exposureAppender,
        Mockito.mock(AdvertisementExposureManager::class.java),
        productReader,
        Mockito.mock(com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader::class.java),
        Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    private fun product(category: AdvertisementProductCategory): AdvertisementProduct =
        Mockito.mock(AdvertisementProduct::class.java).also { Mockito.`when`(it.category).thenReturn(category) }

    private fun createCommand() = CreateAdminAdvertisementExposureCommand(
        productId = PRODUCT_ID,
        contractId = null,
        name = "샘플 배너",
        imageUrl = "https://example.com/banner.png",
        linkUrl = "/jobs/5501",
        targetType = null,
        targetId = null,
        adLabel = true,
        startAt = null,
        endAt = null,
        exposed = true,
    )

    private fun exposureAppendDto() = com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureAppendDto(
        productId = PRODUCT_ID,
        contractId = null,
        content = AdvertisementExposureContent("샘플 배너", "https://example.com/banner.png", "/jobs/5501"),
        adLabel = true,
        period = AdvertisementExposurePeriod.ALWAYS,
        exposed = true,
    )

    private companion object {
        const val PRODUCT_ID = 11L
        const val EXPOSURE_ID = 501L
    }
}
