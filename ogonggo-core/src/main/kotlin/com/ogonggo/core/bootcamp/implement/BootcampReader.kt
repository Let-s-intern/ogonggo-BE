package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import com.ogonggo.core.error.EntityNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

interface BootcampReader {
    fun read(bootcampId: Long): Bootcamp
    fun readPublic(bootcampId: Long): Bootcamp
    fun readPublic(bootcampId: Long, now: LocalDateTime): Bootcamp
    fun readPublicPage(page: Int, size: Int, sortType: BootcampSortType): BootcampPage
    fun readPublicPage(page: Int, size: Int, sortType: BootcampSortType, now: LocalDateTime): BootcampPage
    fun readOwned(ownerUserId: Long, bootcampId: Long): Bootcamp
    fun readOwnedPage(ownerUserId: Long, page: Int, size: Int): BootcampPage
    fun readOwnedForUpdate(ownerUserId: Long, bootcampId: Long): Bootcamp
    fun readOwnedForDelete(ownerUserId: Long, bootcampId: Long): Bootcamp
    fun readForUpdate(bootcampId: Long): Bootcamp
}

@Component
internal class BootcampReaderImpl(
    private val bootcampRepository: BootcampJpaRepository,
    private val clock: Clock,
) : BootcampReader {

    override fun read(bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdAndDeletedAtIsNull(bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    override fun readPublic(bootcampId: Long): Bootcamp =
        readPublic(bootcampId, LocalDateTime.now(clock))

    override fun readPublic(bootcampId: Long, now: LocalDateTime): Bootcamp =
        bootcampRepository.findPublicById(
            bootcampId = bootcampId,
            statuses = PUBLIC_STATUSES,
            now = now,
        ) ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    override fun readPublicPage(page: Int, size: Int, sortType: BootcampSortType): BootcampPage =
        readPublicPage(page, size, sortType, LocalDateTime.now(clock))

    override fun readPublicPage(
        page: Int,
        size: Int,
        sortType: BootcampSortType,
        now: LocalDateTime,
    ): BootcampPage {
        validatePageRequest(page, size)
        val result = when (sortType) {
            BootcampSortType.LATEST -> bootcampRepository.findAllPublic(
                statuses = PUBLIC_STATUSES,
                now = now,
                pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")),
            )

            // 정렬을 JPQL이 이미 정하므로 Pageable에 정렬을 넘기지 않는다.
            BootcampSortType.VIEW_COUNT -> bootcampRepository.findAllPublicOrderByViewCount(
                statuses = PUBLIC_STATUSES,
                now = now,
                pageable = PageRequest.of(page, size),
            )
        }
        return BootcampPage(
            bootcamps = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext(),
        )
    }

    override fun readOwned(ownerUserId: Long, bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(bootcampId, ownerUserId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    override fun readOwnedPage(ownerUserId: Long, page: Int, size: Int): BootcampPage {
        validatePageRequest(page, size)
        val result = bootcampRepository.findAllByOwnerUserIdAndDeletedAtIsNull(
            ownerUserId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")),
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

    override fun readOwnedForUpdate(ownerUserId: Long, bootcampId: Long): Bootcamp =
        bootcampRepository.findOwnedByIdForUpdate(ownerUserId, bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    override fun readOwnedForDelete(ownerUserId: Long, bootcampId: Long): Bootcamp =
        bootcampRepository.findOwnedByIdForDelete(ownerUserId, bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    override fun readForUpdate(bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdForUpdate(bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)
}

private val PUBLIC_STATUSES = listOf(BootcampStatus.RECRUITING, BootcampStatus.CLOSED)

data class BootcampPage(
    val bootcamps: List<Bootcamp>,
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
