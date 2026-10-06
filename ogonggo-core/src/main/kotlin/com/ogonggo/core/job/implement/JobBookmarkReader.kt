package com.ogonggo.core.job.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.job.domain.JobBookmarkSearchCondition
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class JobBookmarkReader internal constructor(
    private val jobQueryRepository: JobQueryRepository,
    private val jobBookmarkRepository: JobBookmarkJpaRepository,
) {

    /**
     * 정렬은 조회 쿼리가 북마크 정렬 기준으로 정하므로 Pageable에는 페이지 범위만 넘긴다.
     */
    fun readBookmarkedPublishedPage(
        userId: Long,
        condition: JobSearchCondition,
        page: Int,
        size: Int,
        bookmarkCondition: JobBookmarkSearchCondition = JobBookmarkSearchCondition.NONE,
    ): JobPageDto {
        validatePageRequest(page, size)
        val result = jobQueryRepository.findBookmarkedPublishedPage(
            userId = userId,
            condition = condition,
            bookmarkCondition = bookmarkCondition,
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

    fun readBookmarkedJobIds(userId: Long, jobIds: Collection<Long>): Set<Long> =
        if (jobIds.isEmpty()) emptySet() else jobBookmarkRepository.findActiveJobIds(userId, jobIds)
}
