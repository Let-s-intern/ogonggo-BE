package com.ogonggo.userapi.advertisementexposure.business

import com.ogonggo.core.advertisementexposure.domain.AdvertisementExposureTargetType
import com.ogonggo.core.advertisementexposure.implement.AdvertisementExposureReader
import com.ogonggo.core.advertisementproduct.domain.AdvertisementProductPosition
import com.ogonggo.core.bootcamp.implement.BootcampBookmarkReader
import com.ogonggo.core.bootcamp.implement.BootcampMetricReader
import com.ogonggo.core.bootcamp.implement.BootcampReader
import com.ogonggo.core.bootcamp.implement.dto.BootcampMetricDto
import com.ogonggo.core.job.domain.JobField
import com.ogonggo.core.job.implement.JobBookmarkReader
import com.ogonggo.core.job.implement.JobMetricReader
import com.ogonggo.core.job.implement.JobReader
import com.ogonggo.core.job.implement.dto.JobMetricDto
import com.ogonggo.userapi.bootcamp.business.UserBootcampSummary
import com.ogonggo.userapi.job.business.UserJobSummary
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDateTime

@Service
class AdvertisementExposureService(
    private val advertisementExposureReader: AdvertisementExposureReader,
    private val jobReader: JobReader,
    private val jobBookmarkReader: JobBookmarkReader,
    private val jobMetricReader: JobMetricReader,
    private val bootcampReader: BootcampReader,
    private val bootcampBookmarkReader: BootcampBookmarkReader,
    private val bootcampMetricReader: BootcampMetricReader,
    private val clock: Clock,
) {

    fun getShown(position: AdvertisementProductPosition, page: Int, size: Int): AdvertisementExposurePageResult =
        AdvertisementExposurePageResult.from(
            advertisementExposureReader.readShownPage(position, LocalDateTime.now(clock), page, size),
        )

    /**
     * 광고 공고 자리에 지금 보이는 카드를 자리 안 순서대로 준다. 자리마다 많아야 몇 건이라 페이지를 나누지 않는다.
     * 노출이 켜져 있고 기간 안이어도 대상 공고가 게시 중·모집 중이 아니면 뺀다.
     * 북마크 여부와 지표는 공개 목록과 같은 방식으로 채우고, 로그인하지 않았으면 북마크가 없는 것으로 본다.
     * 채용공고 목록 자리는 [jobField] 직군 탭의 노출만 준다.
     */
    fun getShownPostings(
        userId: Long?,
        position: AdvertisementProductPosition,
        jobField: JobField? = null,
    ): List<AdvertisementPostingResult> {
        val now = LocalDateTime.now(clock)
        val exposures = advertisementExposureReader.readShown(position, now, jobField)
        fun idsOf(type: AdvertisementExposureTargetType) =
            exposures.filter { it.targetType == type }.mapNotNull { it.targetId }.distinct()
        val jobs = jobSummaries(userId, idsOf(AdvertisementExposureTargetType.JOB), now)
        val bootcamps = bootcampSummaries(userId, idsOf(AdvertisementExposureTargetType.BOOTCAMP), now)
        return exposures.mapNotNull { exposure ->
            val targetType = exposure.targetType ?: return@mapNotNull null
            val targetId = exposure.targetId ?: return@mapNotNull null
            val job = if (targetType == AdvertisementExposureTargetType.JOB) jobs[targetId] ?: return@mapNotNull null else null
            val bootcamp =
                if (targetType == AdvertisementExposureTargetType.BOOTCAMP) bootcamps[targetId] ?: return@mapNotNull null else null
            AdvertisementPostingResult(
                exposureId = checkNotNull(exposure.id),
                position = position,
                jobField = exposure.jobField,
                displayOrder = exposure.displayOrder,
                adLabel = exposure.adLabel,
                targetType = targetType,
                job = job,
                bootcamp = bootcamp,
            )
        }
    }

    private fun jobSummaries(userId: Long?, jobIds: List<Long>, now: LocalDateTime): Map<Long, UserJobSummary> {
        val jobs = jobReader.readPublishedRecruiting(jobIds, now)
        if (jobs.isEmpty()) {
            return emptyMap()
        }
        val ids = jobs.map { checkNotNull(it.id) }
        val bookmarked = if (userId == null) emptySet() else jobBookmarkReader.readBookmarkedJobIds(userId, ids)
        val metrics = jobMetricReader.readAll(ids)
        return jobs.associate { job ->
            val id = checkNotNull(job.id)
            id to UserJobSummary.from(job, id in bookmarked, metrics[id] ?: JobMetricDto.EMPTY)
        }
    }

    private fun bootcampSummaries(userId: Long?, bootcampIds: List<Long>, now: LocalDateTime): Map<Long, UserBootcampSummary> {
        val bootcamps = bootcampReader.readPublicRecruiting(bootcampIds, now)
        if (bootcamps.isEmpty()) {
            return emptyMap()
        }
        val ids = bootcamps.map { checkNotNull(it.id) }
        val bookmarked = if (userId == null) emptySet() else bootcampBookmarkReader.readBookmarkedBootcampIds(userId, ids)
        val metrics = bootcampMetricReader.readAll(ids)
        return bootcamps.associate { bootcamp ->
            val id = checkNotNull(bootcamp.id)
            id to UserBootcampSummary.from(bootcamp, id in bookmarked, metrics[id] ?: BootcampMetricDto.EMPTY)
        }
    }
}
