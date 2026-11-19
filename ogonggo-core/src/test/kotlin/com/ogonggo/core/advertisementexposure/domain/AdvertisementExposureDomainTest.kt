package com.ogonggo.core.advertisementexposure.domain

import com.ogonggo.core.advertisementexposure.error.AdvertisementExposureErrorCode
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProduct
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductCategory
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.error.InvalidValueException
import com.ogonggo.core.job.domain.JobField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class AdvertisementExposureDomainTest {

    @Test
    fun `배너는 이름, 이미지, 링크가 있어야 한다`() {
        assertThrows(InvalidValueException::class.java) {
            exposure(product(AdvertisementProductCategory.BANNER), AdvertisementExposureContent(name = "기획전", linkUrl = "/calendar"))
        }
    }

    @Test
    fun `광고 공고는 띄울 공고가 있어야 한다`() {
        assertThrows(InvalidValueException::class.java) {
            exposure(product(AdvertisementProductCategory.JOB_POSTING), AdvertisementExposureContent())
        }
    }

    @Test
    fun `채용공고 목록 자리의 광고 공고는 직군이 있어야 하고 직군을 저장한다`() {
        // given
        val product = product(AdvertisementProductPosition.JOB_LIST)

        // when
        val exception = assertThrows(InvalidValueException::class.java) { exposure(product, JOB_CONTENT) }
        val exposure = exposure(product, JOB_CONTENT.copy(jobField = JobField.IT_DEVELOPMENT))

        // then
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_JOB_FIELD_INVALID, exception.errorCode)
        assertEquals(JobField.IT_DEVELOPMENT, exposure.jobField)
    }

    @Test
    fun `채용공고 목록이 아닌 광고 공고 자리와 배너는 직군을 둘 수 없다`() {
        // given
        val withJobField = JOB_CONTENT.copy(jobField = JobField.IT_DEVELOPMENT)

        // when, then
        listOf(
            AdvertisementProductPosition.JOB_PICK,
            AdvertisementProductPosition.CALENDAR_DAY,
            AdvertisementProductPosition.MYPAGE_SCRAP,
        ).forEach { position ->
            val exception = assertThrows(InvalidValueException::class.java) { exposure(product(position), withJobField) }
            assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_JOB_FIELD_INVALID, exception.errorCode)
        }
        val banner = assertThrows(InvalidValueException::class.java) {
            exposure(product(AdvertisementProductCategory.BANNER), BANNER_CONTENT.copy(jobField = JobField.IT_DEVELOPMENT))
        }
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_BANNER_CONTENT_REQUIRED, banner.errorCode)
    }

    @Test
    fun `채용공고 목록 노출을 고칠 때도 직군이 있어야 한다`() {
        // given
        val product = product(AdvertisementProductPosition.JOB_LIST)
        val exposure = exposure(product, JOB_CONTENT.copy(jobField = JobField.GAME))

        // when
        val exception = assertThrows(InvalidValueException::class.java) {
            exposure.update(product, null, JOB_CONTENT, adLabel = true, period = AdvertisementExposurePeriod.ALWAYS)
        }
        exposure.update(product, null, JOB_CONTENT.copy(jobField = JobField.SALES), adLabel = true, period = AdvertisementExposurePeriod.ALWAYS)

        // then
        assertEquals(AdvertisementExposureErrorCode.ADVERTISEMENT_EXPOSURE_JOB_FIELD_INVALID, exception.errorCode)
        assertEquals(JobField.SALES, exposure.jobField)
    }

    @Test
    fun `인스타그램·카카오톡 상품은 오공고 화면에 노출하지 않는다`() {
        assertThrows(InvalidValueException::class.java) {
            exposure(product(AdvertisementProductCategory.INSTAGRAM_1), BANNER_CONTENT)
        }
    }

    @Test
    fun `카카오톡 CRM 상품은 오공고 화면에 노출하지 않는다`() {
        assertThrows(InvalidValueException::class.java) {
            exposure(product(AdvertisementProductCategory.KAKAO_CRM), BANNER_CONTENT)
        }
    }

    @Test
    fun `배너 모바일 이미지는 선택이고 없으면 비워 둔다`() {
        // when
        val withMobile = exposure(
            product(AdvertisementProductCategory.BANNER),
            BANNER_CONTENT.copy(mobileImageUrl = "https://example.com/banner-mobile.png"),
        )
        val withoutMobile = exposure(product(AdvertisementProductCategory.BANNER), BANNER_CONTENT)

        // then
        assertEquals("https://example.com/banner-mobile.png", withMobile.mobileImageUrl)
        assertEquals(null, withoutMobile.mobileImageUrl)
    }

    @Test
    fun `배너 모바일 이미지는 보내면 비울 수 없다`() {
        assertThrows(InvalidValueException::class.java) {
            exposure(product(AdvertisementProductCategory.BANNER), BANNER_CONTENT.copy(mobileImageUrl = " "))
        }
    }

    @Test
    fun `광고 공고는 모바일 이미지를 가질 수 없다`() {
        assertThrows(InvalidValueException::class.java) {
            exposure(
                product(AdvertisementProductCategory.JOB_POSTING),
                AdvertisementExposureContent(
                    mobileImageUrl = "https://example.com/banner-mobile.png",
                    targetType = AdvertisementExposureTargetType.JOB,
                    targetId = 1L,
                ),
            )
        }
    }

    @Test
    fun `노출 기간은 시작과 끝이 함께 있거나 함께 없다`() {
        assertThrows(InvalidValueException::class.java) { AdvertisementExposurePeriod(startAt = NOW) }
    }

    @Test
    fun `켜져 있고 기간 안일 때만 보인다`() {
        val period = AdvertisementExposurePeriod(startAt = NOW, endAt = NOW.plusDays(3))
        val shown = exposure(product(AdvertisementProductCategory.BANNER), BANNER_CONTENT, period)

        assertEquals(false, shown.isShownAt(NOW.minusSeconds(1)))
        assertEquals(true, shown.isShownAt(NOW))
        assertEquals(true, shown.isShownAt(NOW.plusDays(3)))
        assertEquals(false, shown.isShownAt(NOW.plusDays(3).plusSeconds(1)))

        shown.changeExposed(false)
        assertEquals(false, shown.isShownAt(NOW.plusDays(1)))
    }

    @Test
    fun `기간이 없으면 상시 보인다`() {
        val always = exposure(product(AdvertisementProductCategory.BANNER), BANNER_CONTENT)

        assertEquals(true, always.isShownAt(NOW.plusYears(10)))
    }

    private fun product(category: AdvertisementProductCategory): AdvertisementProduct {
        val position = AdvertisementProductPosition.entries.firstOrNull { it.category == category }
        return AdvertisementProduct(category, position, "상품", null).also { ReflectionTestUtils.setField(it, "id", 1L) }
    }

    private fun product(position: AdvertisementProductPosition): AdvertisementProduct =
        AdvertisementProduct(position.category, position, "상품", null).also { ReflectionTestUtils.setField(it, "id", 1L) }

    private fun exposure(
        product: AdvertisementProduct,
        content: AdvertisementExposureContent,
        period: AdvertisementExposurePeriod = AdvertisementExposurePeriod.ALWAYS,
    ) = AdvertisementExposure(
        product = product,
        contractId = null,
        content = content,
        adLabel = false,
        period = period,
        exposed = true,
        displayOrder = 1,
    )

    companion object {
        private val NOW = LocalDateTime.of(2026, 11, 7, 12, 0)
        private val JOB_CONTENT = AdvertisementExposureContent(targetType = AdvertisementExposureTargetType.JOB, targetId = 10L)
        private val BANNER_CONTENT = AdvertisementExposureContent(
            name = "오늘의 공고 홍보",
            imageUrl = "https://example.com/banner.png",
            linkUrl = "/calendar",
        )
    }
}
