package com.ogonggo.core.job.implement

import com.ogonggo.core.bookmark.domain.ApplicationStatus
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
     * 정렬은 조회 쿼리가 최근 북마크 순으로 정하므로 Pageable에는 페이지 범위만 넘긴다.
     * 지원·신청 관리 단계를 주면 그 단계의 북마크만 읽고, 주지 않으면 모든 단계를 읽는다.
     */
    fun readBookmarkedPublishedPage(
        userId: Long,
        condition: JobSearchCondition,
        page: Int,
        size: Int,
        applicationStatus: ApplicationStatus? = null,
    ): JobPageDto {
        validateBookmarkPageRequest(page, size)
        val result = jobQueryRepository.findBookmarkedPublishedPage(
            userId = userId,
            condition = condition,
            applicationStatus = applicationStatus,
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

private fun validateBookmarkPageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
