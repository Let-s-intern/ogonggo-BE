package com.ogonggo.core.job.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.EmploymentType
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobManagementSearchCondition
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.job.persistence.JobJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
import com.ogonggo.core.review.domain.ReviewStatus
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

    /** 크롤러가 등록 응답의 식별자를 잃었을 때 원문 URL로 되찾는다. 크롤러는 소유자가 없는 수집 공고만 다룬다. */
    fun readCrawledBySourceUrl(sourceUrl: String): Job =
        jobRepository.findFirstBySourceUrlAndOwnerUserIdIsNullAndDeletedAtIsNullOrderByIdAsc(sourceUrl)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    /** 크롤러는 기업회원 공고를 고칠 수 없으므로 소유자가 없는 공고만 잠가 찾는다. */
    fun readCrawledForUpdate(jobId: Long): Job =
        jobRepository.findCrawledByIdForUpdate(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 수집 공고도 잠가 찾는다. */
    fun readCrawledForDelete(jobId: Long): Job =
        jobRepository.findCrawledByIdForDelete(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

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

    fun readPopularRecruiting(employmentType: EmploymentType?, limit: Int): List<Job> =
        readPopularRecruiting(employmentType, limit, LocalDateTime.now(clock))

    /**
     * 마감됐거나 모집 종료 일시가 지난 공고를 빼고 조회 수가 높은 게시 공고를 limit건까지 읽는다.
     * 고용 형태를 주면 그 고용 형태의 공고만 읽는다.
     */
    fun readPopularRecruiting(employmentType: EmploymentType?, limit: Int, now: LocalDateTime): List<Job> {
        require(limit in 1..100) { "인기 공고 개수는 1 이상 100 이하여야 합니다." }
        return jobQueryRepository.findPopularRecruiting(employmentType, limit, now)
    }

    fun readRecruitingMatched(
        jobRoles: Collection<String>,
        industries: Collection<String>,
        excludedJobIds: Collection<Long>,
        limit: Int,
    ): List<Job> = readRecruitingMatched(jobRoles, industries, excludedJobIds, limit, LocalDateTime.now(clock))

    /** 직무와 산업이 모두 비면 조건 없이 모든 공고를 읽게 되므로 둘 중 하나는 있어야 한다. */
    fun readRecruitingMatched(
        jobRoles: Collection<String>,
        industries: Collection<String>,
        excludedJobIds: Collection<Long>,
        limit: Int,
        now: LocalDateTime,
    ): List<Job> {
        require(limit in 1..100) { "조회 개수는 1 이상 100 이하여야 합니다." }
        require(jobRoles.isNotEmpty() || industries.isNotEmpty()) { "직무나 산업 중 하나는 있어야 합니다." }
        return jobQueryRepository.findRecruitingMatched(jobRoles, industries, excludedJobIds, limit, now)
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

    /** 삭제는 멱등해야 하므로 이미 삭제된 공고도 잠가 찾는다. */
    fun readForDelete(jobId: Long): Job =
        jobRepository.findIncludingDeletedByIdForUpdate(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    fun readManagementPage(
        condition: JobManagementSearchCondition,
        sortType: JobSortType,
        page: Int,
        size: Int,
    ): JobPageDto = readManagementPage(condition, sortType, page, size, LocalDateTime.now(clock))

    /** 게시 상태와 무관하게 미삭제 공고를 읽는다. 모집 상태는 저장하지 않으므로 기준 시각으로 계산한다. */
    fun readManagementPage(
        condition: JobManagementSearchCondition,
        sortType: JobSortType,
        page: Int,
        size: Int,
        now: LocalDateTime,
    ): JobPageDto {
        validatePageRequest(page, size)
        val result = jobQueryRepository.findManagementPage(
            condition = condition,
            sortType = sortType,
            now = now,
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

    /** 밀린 것부터 처리하도록 등록 순서대로 읽는다. 기업회원 공고와 크롤러가 보낸 수집 공고가 함께 나온다. */
    fun readPendingReviews(): List<Job> =
        jobRepository.findAllByReviewStatusAndDeletedAtIsNullOrderByIdAsc(ReviewStatus.PENDING)

    fun countPendingReviews(): Long =
        jobRepository.countByReviewStatusAndDeletedAtIsNull(ReviewStatus.PENDING)

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
