package com.ogonggo.core.job.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class JobReader internal constructor(
    private val jobRepository: JobJpaRepository,
    private val jobQueryRepository: JobQueryRepository,
    private val clock: Clock,
) {

    fun read(jobId: Long): Job =
        jobRepository.findByIdAndDeletedAtIsNull(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    /** 같은 원문에서 이미 수집한 공고가 있는지 확인한다. 삭제된 공고는 다시 등록할 수 있게 제외한다. */

    fun existsBySourceUrl(sourceUrl: String): Boolean =
        jobRepository.existsBySourceUrlAndDeletedAtIsNull(sourceUrl)

    fun readPublished(jobId: Long): Job =
        jobRepository.findByIdAndPublicationStatusAndDeletedAtIsNull(
            id = jobId,
            publicationStatus = JobPublicationStatus.PUBLISHED,
        ) ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    /** 게시 상태와 정렬은 조회 쿼리가 정하므로 Pageable에는 페이지 범위만 넘긴다. */
    fun readPublishedPage(
        condition: JobSearchCondition,
        sortType: JobSortType,
        page: Int,
        size: Int,
    ): JobPageDto {
        validatePageRequest(page, size)
        val result = jobQueryRepository.findPublishedPage(
            condition = condition,
            sortType = sortType,
            pageable = PageRequest.of(page, size),
        )
        return JobPageDto(
            jobs = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    fun readPopularRecruiting(limit: Int): List<Job> =
        readPopularRecruiting(limit, LocalDateTime.now(clock))

    /** 마감됐거나 모집 종료 일시가 지난 공고를 빼고 조회 수가 높은 게시 공고를 limit건까지 읽는다. */
    fun readPopularRecruiting(limit: Int, now: LocalDateTime): List<Job> {
        require(limit in 1..100) { "인기 공고 개수는 1 이상 100 이하여야 합니다." }
        return jobQueryRepository.findPopularRecruiting(limit, now)
    }

    fun readPublishedCalendar(
        rangeStart: LocalDateTime,
        rangeEndExclusive: LocalDateTime,
    ): List<Job> {
        require(rangeStart.isBefore(rangeEndExclusive)) { "달력 조회 시작 일시는 종료 일시보다 빨라야 합니다." }
        return jobRepository.findPublishedCalendarJobs(
            publicationStatus = JobPublicationStatus.PUBLISHED,
            rangeStart = rangeStart,
            rangeEndExclusive = rangeEndExclusive,
        )
    }

    fun readForUpdate(jobId: Long): Job =
        jobRepository.findByIdForUpdate(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    /** 북마크 해제처럼 이미 삭제된 공고에도 허용해야 하는 동작에서만 사용한다. */

    fun readIncludingDeleted(jobId: Long): Job =
        jobRepository.findIncludingDeletedById(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    fun readOwned(ownerUserId: Long, jobId: Long): Job =
        jobRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(jobId, ownerUserId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    fun readOwnedPage(ownerUserId: Long, page: Int, size: Int): JobPageDto {
        validatePageRequest(page, size)
        val result = jobRepository.findAllByOwnerUserIdAndDeletedAtIsNull(
            ownerUserId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")),
        )
        return JobPageDto(
            jobs = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    fun readOwnedForUpdate(ownerUserId: Long, jobId: Long): Job =
        jobRepository.findOwnedByIdForUpdate(ownerUserId, jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    fun readOwnedForDelete(ownerUserId: Long, jobId: Long): Job =
        jobRepository.findOwnedByIdForDelete(ownerUserId, jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)
}

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
