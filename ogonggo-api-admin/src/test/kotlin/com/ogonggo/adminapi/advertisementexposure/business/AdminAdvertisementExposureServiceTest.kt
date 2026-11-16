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
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.implement.dto.AdvertisementExposureUpdateTargetDto
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.image.domain.ImageAssetUsage
import com.ogonggo.core.image.implement.ImageAssetManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
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
        val result = target.createExposure(createCommand(), ADMIN_ID)

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
            target.createExposure(createCommand(), ADMIN_ID)
        }

        // then
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_PRODUCT_NOT_EXPOSABLE, exception.errorCode)
    }

    @Test
    @DisplayName("배너를 등록하면 관리자가 올린 PC·모바일 이미지를 첨부해 정리 작업이 지우지 않게 한다")
    fun `배너 등록은 이미지 URL을 관리자 소유로 첨부한다`() {
        // given
        val productReader = Mockito.mock(AdvertisementProductReader::class.java)
        val exposureAppender = Mockito.mock(AdvertisementExposureAppender::class.java)
        val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
        val bannerProduct = product(AdvertisementProductCategory.BANNER)
        Mockito.`when`(productReader.readForUpdate(PRODUCT_ID)).thenReturn(bannerProduct)
        Mockito.`when`(exposureAppender.append(Mockito.any() ?: exposureAppendDto())).thenReturn(EXPOSURE_ID)
        val target = service(productReader, exposureAppender, imageAssetManager = imageAssetManager)

        // when
        target.createExposure(createCommand().copy(mobileImageUrl = MOBILE_IMAGE_URL), ADMIN_ID)

        // then
        Mockito.verify(imageAssetManager).attachImageUrls(ADMIN_ID, listOf(IMAGE_URL, MOBILE_IMAGE_URL))
    }

    @Test
    @DisplayName("배너를 수정하면 바뀐 이미지도 관리자 소유로 첨부한다")
    fun `배너 수정은 이미지 URL을 관리자 소유로 첨부한다`() {
        // given
        val exposureReader = Mockito.mock(AdvertisementExposureReader::class.java)
        val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
        val updateTarget = AdvertisementExposureUpdateTargetDto(
            exposure = Mockito.mock(AdvertisementExposure::class.java),
            product = product(AdvertisementProductCategory.BANNER),
        )
        Mockito.`when`(exposureReader.readForUpdateWithActiveProduct(EXPOSURE_ID)).thenReturn(updateTarget)
        val target = service(
            Mockito.mock(AdvertisementProductReader::class.java),
            Mockito.mock(AdvertisementExposureAppender::class.java),
            exposureReader = exposureReader,
            imageAssetManager = imageAssetManager,
        )

        // when
        target.updateExposure(updateCommand(), ADMIN_ID)

        // then
        Mockito.verify(imageAssetManager).attachImageUrls(ADMIN_ID, listOf(IMAGE_URL, null))
    }

    @Test
    @DisplayName("배너 이미지를 바꾸면 빠진 이전 이미지만 참조를 풀어 정리 작업이 지우게 한다")
    fun `배너 수정은 바뀐 이전 이미지의 참조를 푼다`() {
        // given
        val exposureReader = Mockito.mock(AdvertisementExposureReader::class.java)
        val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
        val exposure = Mockito.mock(AdvertisementExposure::class.java)
        Mockito.`when`(exposure.imageUrl).thenReturn(IMAGE_URL)
        Mockito.`when`(exposure.mobileImageUrl).thenReturn(MOBILE_IMAGE_URL)
        val updateTarget = AdvertisementExposureUpdateTargetDto(exposure, product(AdvertisementProductCategory.BANNER))
        Mockito.`when`(exposureReader.readForUpdateWithActiveProduct(EXPOSURE_ID)).thenReturn(updateTarget)
        val target = service(
            Mockito.mock(AdvertisementProductReader::class.java),
            Mockito.mock(AdvertisementExposureAppender::class.java),
            exposureReader = exposureReader,
            imageAssetManager = imageAssetManager,
        )

        // when
        target.updateExposure(updateCommand().copy(mobileImageUrl = NEW_MOBILE_IMAGE_URL), ADMIN_ID)

        // then
        Mockito.verify(imageAssetManager).unreferenceImageUrls(listOf(MOBILE_IMAGE_URL), ImageAssetUsage.NOTIFICATION, NOW)
    }

    @Test
    @DisplayName("배너를 지우면 PC·모바일 이미지의 참조를 푼다")
    fun `배너 삭제는 이미지 참조를 푼다`() {
        // given
        val exposureReader = Mockito.mock(AdvertisementExposureReader::class.java)
        val imageAssetManager = Mockito.mock(ImageAssetManager::class.java)
        val exposure = Mockito.mock(AdvertisementExposure::class.java)
        Mockito.`when`(exposure.imageUrl).thenReturn(IMAGE_URL)
        Mockito.`when`(exposure.mobileImageUrl).thenReturn(MOBILE_IMAGE_URL)
        Mockito.`when`(exposureReader.readForUpdate(EXPOSURE_ID)).thenReturn(exposure)
        val target = service(
            Mockito.mock(AdvertisementProductReader::class.java),
            Mockito.mock(AdvertisementExposureAppender::class.java),
            exposureReader = exposureReader,
            imageAssetManager = imageAssetManager,
        )

        // when
        target.deleteExposure(EXPOSURE_ID)

        // then
        Mockito.verify(imageAssetManager).unreferenceImageUrls(listOf(IMAGE_URL, MOBILE_IMAGE_URL), ImageAssetUsage.NOTIFICATION, NOW)
    }

    private fun service(
        productReader: AdvertisementProductReader,
        exposureAppender: AdvertisementExposureAppender,
        exposureReader: AdvertisementExposureReader = Mockito.mock(AdvertisementExposureReader::class.java),
        imageAssetManager: ImageAssetManager = Mockito.mock(ImageAssetManager::class.java),
    ) = AdminAdvertisementExposureService(
        exposureReader,
        exposureAppender,
        Mockito.mock(AdvertisementExposureManager::class.java),
        productReader,
        Mockito.mock(com.ogonggo.core.advertisementcontract.implement.AdvertisementContractReader::class.java),
        imageAssetManager,
        Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    private fun product(category: AdvertisementProductCategory): AdvertisementProduct =
        Mockito.mock(AdvertisementProduct::class.java).also { Mockito.`when`(it.category).thenReturn(category) }

    private fun createCommand() = CreateAdminAdvertisementExposureCommand(
        productId = PRODUCT_ID,
        contractId = null,
        name = "샘플 배너",
        imageUrl = "https://example.com/banner.png",
        mobileImageUrl = null,
        linkUrl = "/jobs/5501",
        targetType = null,
        targetId = null,
        adLabel = true,
        startAt = null,
        endAt = null,
        exposed = true,
    )

    private fun updateCommand() = UpdateAdminAdvertisementExposureCommand(
        exposureId = EXPOSURE_ID,
        contractId = null,
        name = "샘플 배너",
        imageUrl = IMAGE_URL,
        mobileImageUrl = null,
        linkUrl = "/jobs/5501",
        targetType = null,
        targetId = null,
        adLabel = true,
        startAt = null,
        endAt = null,
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
        const val ADMIN_ID = 7L
        const val IMAGE_URL = "https://example.com/banner.png"
        const val MOBILE_IMAGE_URL = "https://example.com/banner-mobile.png"
        const val NEW_MOBILE_IMAGE_URL = "https://example.com/banner-mobile-new.png"
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 8, 9, 0)
    }
}
