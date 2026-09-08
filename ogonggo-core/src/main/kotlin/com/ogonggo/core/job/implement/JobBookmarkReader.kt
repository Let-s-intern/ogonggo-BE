package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobPublicationStatus
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import com.ogonggo.core.job.persistence.JobJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class JobBookmarkReader internal constructor(
    private val jobRepository: JobJpaRepository,
    private val jobBookmarkRepository: JobBookmarkJpaRepository,
) {

    fun readBookmarkedPublishedPage(userId: Long, page: Int, size: Int): JobPage {
        validateBookmarkPageRequest(page, size)
        val result = jobRepository.findBookmarkedJobs(
            userId = userId,
            publicationStatus = JobPublicationStatus.PUBLISHED,
            pageable = PageRequest.of(page, size),
        )
        return JobPage(
            jobs = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    fun readBookmarkedJobIds(userId: Long, jobIds: Collection<Long>): Set<Long> =
        if (jobIds.isEmpty()) emptySet() else jobBookmarkRepository.findActiveJobIds(userId, jobIds)
}

private fun validateBookmarkPageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
