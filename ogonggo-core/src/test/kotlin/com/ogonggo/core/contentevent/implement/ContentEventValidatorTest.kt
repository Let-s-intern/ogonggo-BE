package com.ogonggo.core.contentevent.implement

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposure
import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.contentevent.domain.ContentEventSource
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.JobReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDateTime

class ContentEventValidatorTest {

    private val jobReader = Mockito.mock(JobReader::class.java)
    private val bootcampReader = Mockito.mock(BootcampReader::class.java)
    private val exposureRepository = Mockito.mock(AdvertisementExposureJpaRepository::class.java)
    private val validator = ContentEventValidator(jobReader, bootcampReader, exposureRepository)

    @Test
    fun `게시 중이 아니거나 없는 대상의 이벤트는 그 건만 버린다`() {
        // given
        Mockito.`when`(jobReader.readPublished(HIDDEN_JOB_ID)).thenThrow(EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND))
        Mockito.`when`(bootcampReader.readPublic(MISSING_BOOTCAMP_ID, NOW))
            .thenThrow(EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND))
        val shown = view(ContentEventTargetType.JOB, JOB_ID)
        val hidden = view(ContentEventTargetType.JOB, HIDDEN_JOB_ID)
        val missingBootcamp = view(ContentEventTargetType.BOOTCAMP, MISSING_BOOTCAMP_ID)

        // when
        val storable = validator.filterStorable(listOf(shown, hidden, missingBootcamp, shown), NOW)

        // then
        assertEquals(listOf(shown, shown), storable)
    }

    @Test
    fun `종류에 필요한 값이 없거나 스크롤이 아닌데 스크롤 값이 있으면 버린다`() {
        val noTarget = view(ContentEventTargetType.JOB, JOB_ID).copy(targetType = null, targetId = null)
        val impressionWithoutExposure = view(ContentEventTargetType.JOB, JOB_ID).copy(eventType = ContentEventType.IMPRESSION)
        val viewWithScroll = view(ContentEventTargetType.JOB, JOB_ID).copy(scrollPercent = 40)
        val scroll = view(ContentEventTargetType.JOB, JOB_ID).copy(eventType = ContentEventType.SCROLL, scrollPercent = 40, durationMs = 1000L)

        assertEquals(listOf(scroll), validator.filterStorable(listOf(noTarget, impressionWithoutExposure, viewWithScroll, scroll), NOW))
    }

    @Test
    fun `노출은 지우지 않았고 광고 공고면 대상과 같아야 한다`() {
        // given
        val jobPostingExposure = exposure(AdvertisementExposureTargetType.JOB, JOB_ID)
        val bannerExposure = exposure(targetType = null, targetId = null)
        Mockito.`when`(exposureRepository.findByIdAndDeletedAtIsNull(AD_CARD_EXPOSURE_ID)).thenReturn(jobPostingExposure)
        Mockito.`when`(exposureRepository.findByIdAndDeletedAtIsNull(BANNER_EXPOSURE_ID)).thenReturn(bannerExposure)
        val matched = view(ContentEventTargetType.JOB, JOB_ID).copy(exposureId = AD_CARD_EXPOSURE_ID)
        val otherTarget = view(ContentEventTargetType.JOB, OTHER_JOB_ID).copy(exposureId = AD_CARD_EXPOSURE_ID)
        val otherType = view(ContentEventTargetType.BOOTCAMP, JOB_ID).copy(exposureId = AD_CARD_EXPOSURE_ID)
        val deletedExposure = view(ContentEventTargetType.JOB, JOB_ID).copy(exposureId = DELETED_EXPOSURE_ID)
        val bannerClick = ContentEventAppendDto(
            eventType = ContentEventType.AD_CLICK,
            targetType = null,
            targetId = null,
            exposureId = BANNER_EXPOSURE_ID,
            userId = null,
            visitorId = VISITOR_ID,
            source = ContentEventSource.BANNER,
        )
        val viewFromBanner = view(ContentEventTargetType.JOB, OTHER_JOB_ID).copy(exposureId = BANNER_EXPOSURE_ID)

        // when
        val storable = validator.filterStorable(
            listOf(matched, otherTarget, otherType, deletedExposure, bannerClick, viewFromBanner),
            NOW,
        )

        // then
        assertEquals(listOf(matched, bannerClick, viewFromBanner), storable)
    }

    private fun exposure(targetType: AdvertisementExposureTargetType?, targetId: Long?): AdvertisementExposure =
        Mockito.mock(AdvertisementExposure::class.java).also {
            Mockito.`when`(it.targetType).thenReturn(targetType)
            Mockito.`when`(it.targetId).thenReturn(targetId)
        }

    private fun view(targetType: ContentEventTargetType, targetId: Long) = ContentEventAppendDto(
        eventType = ContentEventType.VIEW,
        targetType = targetType,
        targetId = targetId,
        exposureId = null,
        userId = null,
        visitorId = VISITOR_ID,
        source = ContentEventSource.LIST,
    )

    private companion object {
        const val VISITOR_ID = "5f0c2e9e-1b7a-4c1e-9a43-0d2f3b6c7a11"
        const val JOB_ID = 15818L
        const val OTHER_JOB_ID = 15819L
        const val HIDDEN_JOB_ID = 20L
        const val MISSING_BOOTCAMP_ID = 30L
        const val AD_CARD_EXPOSURE_ID = 31L
        const val BANNER_EXPOSURE_ID = 12L
        const val DELETED_EXPOSURE_ID = 99L
        val NOW: LocalDateTime = LocalDateTime.of(2026, 11, 19, 10, 0)
    }
}
