package com.ogonggo.core.job.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.job.domain.Job
import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.domain.JobSortType
import com.ogonggo.core.job.error.JobErrorCode
import com.ogonggo.core.job.persistence.JobJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface JobReader {
    fun read(jobId: Long): Job

    /** 같은 원문에서 이미 수집한 공고가 있는지 확인한다. 삭제된 공고는 다시 등록할 수 있게 제외한다. */
    fun existsBySourceUrl(sourceUrl: String): Boolean
    fun readPublished(jobId: Long): Job
    fun readPublishedPage(page: Int, size: Int, sortType: JobSortType): JobPage
    fun readPublishedCalendar(rangeStart: LocalDateTime, rangeEndExclusive: LocalDateTime): List<Job>
    fun readForUpdate(jobId: Long): Job
    fun readPublishedForUpdate(jobId: Long): Job
    fun readIncludingDeletedForUpdate(jobId: Long): Job
}

@Component
internal class JobReaderImpl(
    private val jobRepository: JobJpaRepository,
) : JobReader {

    override fun read(jobId: Long): Job =
        jobRepository.findByIdAndDeletedAtIsNull(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    override fun existsBySourceUrl(sourceUrl: String): Boolean =
        jobRepository.existsBySourceUrlAndDeletedAtIsNull(sourceUrl)

    override fun readPublished(jobId: Long): Job =
        jobRepository.findByIdAndPublicationStatusAndDeletedAtIsNull(
            id = jobId,
            publicationStatus = JobPublicationStatus.PUBLISHED,
        ) ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    override fun readPublishedPage(page: Int, size: Int, sortType: JobSortType): JobPage {
        validatePageRequest(page, size)
        val result = when (sortType) {
            JobSortType.LATEST -> jobRepository.findAllByPublicationStatusAndDeletedAtIsNull(
                publicationStatus = JobPublicationStatus.PUBLISHED,
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")),
            )

            // 정렬을 JPQL이 이미 정하므로 Pageable에 정렬을 넘기지 않는다.
            JobSortType.VIEW_COUNT -> jobRepository.findAllPublishedOrderByViewCount(
                publicationStatus = JobPublicationStatus.PUBLISHED,
                pageable = PageRequest.of(page, size),
            )
        }
        return JobPage(
            jobs = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    override fun readPublishedCalendar(
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

    override fun readForUpdate(jobId: Long): Job =
        jobRepository.findByIdForUpdate(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    override fun readPublishedForUpdate(jobId: Long): Job =
        jobRepository.findPublishedByIdForUpdate(jobId, JobPublicationStatus.PUBLISHED)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)

    override fun readIncludingDeletedForUpdate(jobId: Long): Job =
        jobRepository.findIncludingDeletedByIdForUpdate(jobId)
            ?: throw EntityNotFoundException(JobErrorCode.JOB_NOT_FOUND)
}

data class JobPage(
    val jobs: List<Job>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
