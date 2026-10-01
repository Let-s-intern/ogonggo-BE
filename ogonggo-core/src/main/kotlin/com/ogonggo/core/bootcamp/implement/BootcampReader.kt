package com.ogonggo.core.bootcamp.implement

import com.ogonggo.core.bootcamp.domain.Bootcamp
import com.ogonggo.core.bootcamp.domain.BootcampManagementSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampPublicationStatus
import com.ogonggo.core.bootcamp.domain.BootcampSearchCondition
import com.ogonggo.core.bootcamp.domain.BootcampSortType
import com.ogonggo.core.bootcamp.domain.BootcampStatus
import com.ogonggo.core.bootcamp.error.BootcampErrorCode
import com.ogonggo.core.bootcamp.implement.dto.BootcampPageDto
import com.ogonggo.core.bootcamp.persistence.BootcampJpaRepository
import com.ogonggo.core.bootcamp.persistence.BootcampQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.review.domain.ContentSource
import com.ogonggo.core.review.domain.ReviewStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class BootcampReader internal constructor(
    private val bootcampRepository: BootcampJpaRepository,
    private val bootcampQueryRepository: BootcampQueryRepository,
    private val clock: Clock,
) {

    fun read(bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdAndDeletedAtIsNull(bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun existsBySourceUrl(sourceUrl: String): Boolean =
        bootcampRepository.existsBySourceUrlAndDeletedAtIsNull(sourceUrl)

    /** 같은 곳에서 같은 식별값으로 이미 수집한 부트캠프가 있는지 확인한다. 삭제된 부트캠프도 센다. */
    fun existsByExternalId(source: ContentSource, externalId: String): Boolean =
        bootcampRepository.existsBySourceAndExternalId(source, externalId)

    /** 크롤러가 등록 응답의 식별자를 잃었을 때 원문 URL로 되찾는다. 크롤러는 자신이 등록한 부트캠프만 다룬다. */
    fun readCrawledBySourceUrl(sourceUrl: String): Bootcamp =
        bootcampRepository.findFirstBySourceUrlAndSourceAndDeletedAtIsNullOrderByIdAsc(sourceUrl, ContentSource.CRAWLER)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    /**
     * 크롤러는 기업회원·고용24 부트캠프를 고칠 수 없으므로 크롤링 경로이고 원문 URL이 있는 부트캠프만 잠가 찾는다.
     * 원문 URL이 없는 크롤링 경로 부트캠프는 크롤러가 넣은 것이 아니다.
     */
    fun readCrawledForUpdate(bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdAndSourceForUpdate(bootcampId, ContentSource.CRAWLER)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    /** 삭제는 멱등해야 하므로 이미 삭제된 수집 부트캠프도 잠가 찾는다. */
    fun readCrawledForDelete(bootcampId: Long): Bootcamp =
        bootcampRepository.findIncludingDeletedByIdAndSourceForUpdate(bootcampId, ContentSource.CRAWLER)
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
            publicationStatus = BootcampPublicationStatus.PUBLISHED,
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
        return bootcampQueryRepository.findPublicPage(
            condition = condition,
            sortType = sortType,
            publicStatuses = PUBLIC_STATUSES,
            now = now,
            pageable = PageRequest.of(page, size),
        ).toPageDto()
    }

    /** 공개 여부와 무관하게 미삭제 부트캠프를 읽는다. */
    fun readManagementPage(
        condition: BootcampManagementSearchCondition,
        sortType: BootcampSortType,
        page: Int,
        size: Int,
    ): BootcampPageDto {
        validatePageRequest(page, size)
        return bootcampQueryRepository.findManagementPage(
            condition = condition,
            sortType = sortType,
            pageable = PageRequest.of(page, size),
        ).toPageDto()
    }

    /** 밀린 것부터 처리하도록 등록 순서대로 읽는다. 검수 상태는 기업회원 부트캠프에만 있다. */
    fun readPendingReviews(): List<Bootcamp> =
        bootcampRepository.findAllByReviewStatusAndDeletedAtIsNullOrderByIdAsc(ReviewStatus.PENDING)

    fun countPendingReviews(): Long =
        bootcampRepository.countByReviewStatusAndDeletedAtIsNull(ReviewStatus.PENDING)

    fun readOwned(ownerUserId: Long, bootcampId: Long): Bootcamp =
        bootcampRepository.findByIdAndOwnerUserIdAndDeletedAtIsNull(bootcampId, ownerUserId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)

    fun readOwnedPage(ownerUserId: Long, page: Int, size: Int): BootcampPageDto {
        validatePageRequest(page, size)
        return bootcampRepository.findAllByOwnerUserIdAndDeletedAtIsNull(
            ownerUserId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")),
        ).toPageDto()
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

    /** 하나라도 없거나 삭제됐으면 일부만 바뀌지 않도록 모두 거절한다. 같은 식별자가 여러 번 와도 한 번만 읽는다. */
    fun readAllForUpdate(bootcampIds: Collection<Long>): List<Bootcamp> {
        val distinctIds = bootcampIds.toSet()
        require(distinctIds.isNotEmpty()) { "잠글 부트캠프 식별자가 없습니다." }
        val bootcamps = bootcampRepository.findAllByIdInForUpdate(distinctIds)
        if (bootcamps.size != distinctIds.size) {
            throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)
        }
        return bootcamps
    }

    /** 삭제는 멱등해야 하므로 이미 삭제된 부트캠프도 잠가 찾는다. */
    fun readForDelete(bootcampId: Long): Bootcamp =
        bootcampRepository.findIncludingDeletedByIdForUpdate(bootcampId)
            ?: throw EntityNotFoundException(BootcampErrorCode.BOOTCAMP_NOT_FOUND)
}

/** 북마크 목록도 같은 공개 조건을 따르므로 Reader 밖에서도 사용한다. */
internal val PUBLIC_STATUSES = listOf(BootcampStatus.RECRUITING, BootcampStatus.CLOSED)

private fun Page<Bootcamp>.toPageDto(): BootcampPageDto = BootcampPageDto(
    bootcamps = content,
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    hasNext = hasNext(),
)

private fun validatePageRequest(page: Int, size: Int) {
    require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
    require(size in 1..100) { "페이지 크기는 1 이상 100 이하여야 합니다." }
}
