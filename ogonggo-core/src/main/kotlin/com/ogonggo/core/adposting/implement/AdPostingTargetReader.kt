package com.ogonggo.core.adposting.implement

import com.ogonggo.core.adposting.domain.AdPosting
import com.ogonggo.core.adposting.domain.AdPostingTargetType
import com.ogonggo.core.adposting.implement.dto.AdPostingTargetDto
import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampRecruitmentStatus
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobRecruitmentStatus
import com.ogonggo.core.job.persistence.JobJpaRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 광고 공고의 대상(채용공고·부트캠프)을 읽고 지금 사용자에게 나갈 수 있는지 정한다. 관리자 표, 광고주 광고 수,
 * 사용자 공개 조회가 같은 규칙을 쓴다.
 *
 * - 채용공고: 게시 중이고, 마감 처리되지 않았고, 모집 종료 일시가 지나지 않았다. 자동 마감 작업을 기다리지 않고 [now]로 본다.
 * - 부트캠프: 게시 중이고 게시 기간 안이며, 모집 상태가 모집 중이고 모집 종료 일시가 지나지 않았다.
 */
@Component
class AdPostingTargetReader internal constructor(
    private val jobRepository: JobJpaRepository,
    private val bootcampRepository: BootcampJpaRepository,
) {

    /** 지우지 않은 대상이 있는가. 공개·모집 여부는 보지 않는다. */
    fun exists(targetType: AdPostingTargetType, targetId: Long): Boolean = when (targetType) {
        AdPostingTargetType.JOB -> jobRepository.findByIdAndDeletedAtIsNull(targetId) != null
        AdPostingTargetType.BOOTCAMP -> bootcampRepository.findByIdAndDeletedAtIsNull(targetId) != null
    }

    /** 광고 공고 식별자마다 대상을 준다. 대상이 지워졌으면 [AdPostingTargetDto.MISSING]이다. */
    fun read(adPostings: Collection<AdPosting>, now: LocalDateTime): Map<Long, AdPostingTargetDto> {
        val jobs = readJobs(adPostings.filter { it.targetType == AdPostingTargetType.JOB }.map { it.targetId })
        val bootcamps = readBootcamps(adPostings.filter { it.targetType == AdPostingTargetType.BOOTCAMP }.map { it.targetId })
        return adPostings.associate { adPosting ->
            val target = when (adPosting.targetType) {
                AdPostingTargetType.JOB -> jobs[adPosting.targetId]?.let { job ->
                    AdPostingTargetDto(job.title, job.companyName, isAvailable(job, now), job = job)
                }
                AdPostingTargetType.BOOTCAMP -> bootcamps[adPosting.targetId]?.let { bootcamp ->
                    AdPostingTargetDto(bootcamp.title, bootcamp.companyName, isAvailable(bootcamp, now), bootcamp = bootcamp)
                }
            }
            checkNotNull(adPosting.id) { "광고 공고 식별자가 없습니다." } to (target ?: AdPostingTargetDto.MISSING)
        }
    }

    private fun readJobs(jobIds: Collection<Long>): Map<Long, Job> =
        if (jobIds.isEmpty()) {
            emptyMap()
        } else {
            jobRepository.findAllById(jobIds.toSet()).filter { it.deletedAt == null }.associateBy { checkNotNull(it.id) }
        }

    private fun readBootcamps(bootcampIds: Collection<Long>): Map<Long, Bootcamp> =
        if (bootcampIds.isEmpty()) {
            emptyMap()
        } else {
            bootcampRepository.findAllById(bootcampIds.toSet()).filter { it.deletedAt == null }.associateBy { checkNotNull(it.id) }
        }

    private fun isAvailable(job: Job, now: LocalDateTime): Boolean =
        job.publicationStatus == JobPublicationStatus.PUBLISHED &&
            job.recruitmentStatus == JobRecruitmentStatus.RECRUITING &&
            job.closedAt == null &&
            job.recruitmentEndAt?.isBefore(now) != true

    private fun isAvailable(bootcamp: Bootcamp, now: LocalDateTime): Boolean =
        bootcamp.publicationStatus == BootcampPublicationStatus.PUBLISHED &&
            bootcamp.publicationStartAt?.isAfter(now) != true &&
            bootcamp.publicationEndAt?.isBefore(now) != true &&
            bootcamp.status == BootcampRecruitmentStatus.RECRUITING &&
            bootcamp.recruitmentEndAt?.isBefore(now) != true
}
