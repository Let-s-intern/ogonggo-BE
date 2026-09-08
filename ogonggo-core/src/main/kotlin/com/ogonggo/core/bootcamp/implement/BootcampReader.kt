package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.implement.dto.BootcampPageDto
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class BootcampReader internal constructor(
    private val bootcampRepository: BootcampJpaRepository,
    private val bootcampQueryRepository: BootcampQueryRepository,
    private val clock: Clock,
) {

    fun read(bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdAndDeletedAtIsNull(bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun readIncludingDeleted(bootcampId: Long): Bootcamp =
        bootcampRepository.findIncludingDeletedById(bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun readPublic(bootcampId: Long): Bootcamp =
        readPublic(bootcampId, LocalDateTime.now(clock))

    fun readPublic(bootcampId: Long, now: LocalDateTime): Bootcamp =
        bootcampRepository.findPublicById(
            bootcampId = bootcampId,
            statuses = PUBLIC_STATUSES,
            now = now,
        ) ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun readPublicPage(
        condition: BootcampSearchCondition,
        sortType: BootcampSortType,
        page: Int,
        size: Int,
    ): BootcampPageDto = readPublicPage(condition, sortType, page, size, LocalDateTime.now(clock))

    /** 공개 조건과 정렬은 조회 쿼리가 정하므로 Pageable에는 페이지 범위만 넘긴다. */
    fun readPublicPage(
        condition: BootcampSearchCondition,
        sortType: BootcampSortType,
        page: Int,
        size: Int,
        now: LocalDateTime,
    ): BootcampPageDto {
        validatePageRequest(page, size)
        val result = bootcampQueryRepository.findPublicPage(
            condition = condition,
            sortType = sortType,
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

    fun readOwned(ownerUserId: Long, bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(bootcampId, ownerUserId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun readOwnedPage(ownerUserId: Long, page: Int, size: Int): BootcampPageDto {
        validatePageRequest(page, size)
        val result = bootcampRepository.findAllByOwnerUserIdAndDeletedAtIsNull(
            ownerUserId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")),
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

    fun readOwnedForUpdate(ownerUserId: Long, bootcampId: Long): Bootcamp =
        bootcampRepository.findOwnedByIdForUpdate(ownerUserId, bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun readOwnedForDelete(ownerUserId: Long, bootcampId: Long): Bootcamp =
        bootcampRepository.findOwnedByIdForDelete(ownerUserId, bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun readForUpdate(bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdForUpdate(bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)
}

/** 북마크 목록도 같은 공개 조건을 따르므로 Reader 밖에서도 사용한다. */
internal val PUBLIC_STATUSES = listOf(BootcampStatus.RECRUITING, BootcampStatus.CLOSED)

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
