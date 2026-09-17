package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.implement.dto.BootcampPageDto
import com.ogonggo.core.bootcamp.persistence.BootcampBookmarkJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampQueryRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class BootcampBookmarkReader internal constructor(
    private val bootcampQueryRepository: BootcampQueryRepository,
    private val bootcampBookmarkRepository: BootcampBookmarkJpaRepository,
    private val clock: Clock,
) {

    fun readBookmarkedPublicPage(
        userId: Long,
        condition: BootcampSearchCondition,
        page: Int,
        size: Int,
    ): BootcampPageDto = readBookmarkedPublicPage(userId, condition, page, size, LocalDateTime.now(clock))

    fun readBookmarkedPublicPage(
        userId: Long,
        condition: BootcampSearchCondition,
        page: Int,
        size: Int,
        now: LocalDateTime,
    ): BootcampPageDto {
        validateBookmarkPageRequest(page, size)
        // 정렬은 조회 쿼리가 최근 북마크 순으로 정하므로 Pageable에 정렬을 넘기지 않는다.
        val result = bootcampQueryRepository.findBookmarkedPublicPage(
            userId = userId,
            condition = condition,
            publicStatuses = PUBLIC_STATUSES,
            now = now,
            pageable = PageRequest.of(page, size),
        )
        return BootcampPageDto(
            bootcamps = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    fun readBookmarkedBootcampIds(userId: Long, bootcampIds: Collection<Long>): Set<Long> =
        if (bootcampIds.isEmpty()) {
            emptySet()
        } else {
            bootcampBookmarkRepository.findActiveBootcampIds(userId, bootcampIds)
        }
}

private fun validateBookmarkPageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
