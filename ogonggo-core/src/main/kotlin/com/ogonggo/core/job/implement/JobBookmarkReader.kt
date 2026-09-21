package com.ogonggo.core.job.implement

import com.ogonggo.core.job.domain.JobBookmarkSearchCondition
import com.ogonggo.core.job.domain.JobSearchCondition
import com.ogonggo.core.job.implement.dto.JobPageDto
import com.ogonggo.core.job.persistence.JobBookmarkJpaRepository
import com.ogonggo.core.job.persistence.JobQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class JobBookmarkReader internal constructor(
    private val jobQueryRepository: JobQueryRepository,
    private val jobBookmarkRepository: JobBookmarkJpaRepository,
) {

    /**
     * 정렬은 조회 쿼리가 북마크 정렬 기준으로 정하므로 Pageable에는 페이지 범위만 넘긴다.
     * 모집 상태는 저장하지 않고 마감 처리 일시와 모집 종료 일시로 계산하므로 기준 시각이 필요하다.
     */
    fun readBookmarkedPublishedPage(
        userId: Long,
        condition: JobSearchCondition,
        page: Int,
        size: Int,
        now: LocalDateTime,
        bookmarkCondition: JobBookmarkSearchCondition = JobBookmarkSearchCondition.NONE,
    ): JobPageDto {
        validateBookmarkPageRequest(page, size)
        val result = jobQueryRepository.findBookmarkedPublishedPage(
            userId = userId,
            condition = condition,
            bookmarkCondition = bookmarkCondition,
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

    fun readBookmarkedJobIds(userId: Long, jobIds: Collection<Long>): Set<Long> =
        if (jobIds.isEmpty()) emptySet() else jobBookmarkRepository.findActiveJobIds(userId, jobIds)
}

private fun validateBookmarkPageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
