package com.ogonggo.core.contentevent.implement

import com.ogonggo.core.advertisementexposure.persistence.AdvertisementExposureJpaRepository
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.concern.implement.ConcernReader
import com.ogonggo.core.contentevent.domain.ContentEventTargetType
import com.ogonggo.core.contentevent.domain.ContentEventType
import com.ogonggo.core.contentevent.implement.dto.ContentEventAppendDto
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostReader
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ContentEventValidator internal constructor(
    private val jobReader: JobReader,
    private val bootcampReader: BootcampReader,
    private val concernReader: ConcernReader,
    private val recruitmentPostReader: RecruitmentPostReader,
    private val advertisementExposureRepository: AdvertisementExposureJpaRepository,
) {

    /**
     * 저장할 이벤트만 남긴다. 걸린 건은 오류 없이 그 건만 버린다.
     *
     * - 종류에 필요한 대상·노출이 없거나, 스크롤이 아닌데 스크롤 값이 있으면 버린다.
     * - 대상이 없거나 지금 사용자에게 보이지 않으면(게시 중이 아님) 버린다.
     * - 노출이 없거나 지워졌으면 버린다. 노출이 공고를 띄우는 광고 공고이면 이벤트의 대상과 같아야 한다.
     *
     * 한 요청 안에서 같은 대상·노출은 한 번만 읽는다.
     */
    fun filterStorable(events: List<ContentEventAppendDto>, now: LocalDateTime): List<ContentEventAppendDto> {
        val targetVisible = mutableMapOf<Pair<ContentEventTargetType, Long>, Boolean>()
        val exposureTargets = mutableMapOf<Long, ExposureTarget?>()
        return events.filter { event ->
            hasRequiredValues(event) &&
                (event.targetType == null || targetVisible.getOrPut(event.targetType to event.targetId!!) { isVisible(event.targetType, event.targetId, now) }) &&
                (event.exposureId == null || matchesExposure(event, exposureTargets.getOrPut(event.exposureId) { readExposureTarget(event.exposureId) }))
        }
    }

    private fun hasRequiredValues(event: ContentEventAppendDto): Boolean =
        (event.targetType == null) == (event.targetId == null) &&
            (!event.eventType.requiresTarget || event.targetId != null) &&
            (!event.eventType.requiresExposure || event.exposureId != null) &&
            (event.eventType == ContentEventType.SCROLL || (event.scrollPercent == null && event.durationMs == null))

    private fun isVisible(targetType: ContentEventTargetType, targetId: Long, now: LocalDateTime): Boolean =
        try {
            when (targetType) {
                ContentEventTargetType.JOB -> jobReader.readPublished(targetId)
                ContentEventTargetType.BOOTCAMP -> bootcampReader.readPublic(targetId, now)
                ContentEventTargetType.CONCERN -> concernReader.read(targetId)
                ContentEventTargetType.RECRUITMENT_POST -> recruitmentPostReader.readPublished(targetId)
            }
            true
        } catch (exception: EntityNotFoundException) {
            false
        }

    /** 없거나 지운 노출이면 null이다. */
    private fun readExposureTarget(exposureId: Long): ExposureTarget? =
        advertisementExposureRepository.findByIdAndDeletedAtIsNull(exposureId)?.let {
            ExposureTarget(targetTypeName = it.targetType?.name, targetId = it.targetId)
        }

    private fun matchesExposure(event: ContentEventAppendDto, exposure: ExposureTarget?): Boolean {
        if (exposure == null) return false
        if (exposure.targetTypeName == null || event.targetType == null) return true
        return exposure.targetTypeName == event.targetType.name && exposure.targetId == event.targetId
    }

    private data class ExposureTarget(val targetTypeName: String?, val targetId: Long?)
}
