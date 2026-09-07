package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.persistence.BootcampBookmarkJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

interface BootcampBookmarkReader {
    fun readBookmarkedPublicPage(userId: Long, page: Int, size: Int): BootcampPage
    fun readBookmarkedPublicPage(userId: Long, page: Int, size: Int, now: LocalDateTime): BootcampPage
    fun readBookmarkedBootcampIds(userId: Long, bootcampIds: Collection<Long>): Set<Long>
}

@Component
internal class BootcampBookmarkReaderImpl(
    private val bootcampRepository: BootcampJpaRepository,
    private val bootcampBookmarkRepository: BootcampBookmarkJpaRepository,
    private val clock: Clock,
) : BootcampBookmarkReader {

    override fun readBookmarkedPublicPage(userId: Long, page: Int, size: Int): BootcampPage =
        readBookmarkedPublicPage(userId, page, size, LocalDateTime.now(clock))

    override fun readBookmarkedPublicPage(
        userId: Long,
        page: Int,
        size: Int,
        now: LocalDateTime,
    ): BootcampPage {
        validateBookmarkPageRequest(page, size)
        // 정렬을 JPQL이 이미 정하므로 Pageable에 정렬을 넘기지 않는다.
        val result = bootcampRepository.findBookmarkedBootcamps(
            userId = userId,
            statuses = PUBLIC_STATUSES,
            now = now,
            pageable = PageRequest.of(page, size),
        )
        return BootcampPage(
            bootcamps = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    override fun readBookmarkedBootcampIds(userId: Long, bootcampIds: Collection<Long>): Set<Long> =
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
