package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernConsoleSearchCondition
import com.ogonggo.core.concern.domain.ConcernPopularSortType
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.concern.error.ConcernErrorCode
import com.ogonggo.core.concern.implement.dto.ConcernPageDto
import com.ogonggo.core.concern.persistence.ConcernJpaRepository
import com.ogonggo.core.concern.persistence.ConcernQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.paging.validatePageRequest
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ConcernReader internal constructor(
    private val concernRepository: ConcernJpaRepository,
    private val concernQueryRepository: ConcernQueryRepository,
) {

    /** 사용자에게 보이는 고민글만 읽는다. 숨긴 고민글은 없는 글과 같다. */
    fun read(concernId: Long): Concern =
        concernRepository.findByIdAndHiddenFalseAndDeletedAtIsNull(concernId) ?: throw notFound()

    /** 관리자 콘솔은 숨긴 고민글도 읽는다. */
    fun readIncludingHidden(concernId: Long): Concern =
        concernRepository.findByIdAndDeletedAtIsNull(concernId) ?: throw notFound()

    fun readForUpdate(concernId: Long): Concern =
        concernRepository.findActiveByIdForUpdate(concernId) ?: throw notFound()

    /** 삭제된 고민글도 읽는다. 삭제 요청을 반복해도 같은 결과를 주는 데만 쓴다. */
    fun readIncludingDeletedForUpdate(concernId: Long): Concern =
        concernRepository.findByIdForUpdate(concernId) ?: throw notFound()

    fun readConsolePage(
        condition: ConcernConsoleSearchCondition,
        sortType: ConcernSortType,
        page: Int,
        size: Int,
    ): ConcernPageDto {
        validatePageRequest(page, size)
        return concernQueryRepository.findConsolePage(condition, sortType, page, size).toPageDto()
    }

    /** 숨긴 고민글을 포함한 미삭제 고민글을 잠가 읽는다. 하나라도 없으면 없는 식별자를 모두 담아 실패한다. */
    fun readAllIncludingHiddenForUpdate(concernIds: Collection<Long>): List<Concern> {
        val distinctIds = concernIds.toSet()
        require(distinctIds.isNotEmpty()) { "잠글 고민글 식별자가 없습니다." }
        val concerns = concernRepository.findAllByIdInForUpdate(distinctIds)
        val missingIds = distinctIds - concerns.mapNotNullTo(HashSet()) { it.id }
        if (missingIds.isNotEmpty()) {
            throw EntityNotFoundException(
                ConcernErrorCode.CONCERN_NOT_FOUND,
                "${ConcernErrorCode.CONCERN_NOT_FOUND.message} (id: ${missingIds.sorted().joinToString()})",
            )
        }
        return concerns
    }

    fun readPage(
        category: ConcernCategory?,
        sortType: ConcernSortType,
        page: Int,
        size: Int,
    ): ConcernPageDto {
        validatePageRequest(page, size)
        return concernQueryRepository.findPage(category, sortType, page, size).toPageDto()
    }

    /** [createdFrom] 이후에 등록한 고민글 중 조회 수나 답변 수가 많은 것을 [limit]건까지 읽는다. */
    fun readPopular(createdFrom: LocalDateTime, sortType: ConcernPopularSortType, limit: Int): List<Concern> {
        require(limit in 1..100) { "인기 고민글 개수는 1 이상 100 이하여야 합니다." }
        return concernQueryRepository.findPopular(createdFrom, sortType, limit)
    }

    private fun Page<Concern>.toPageDto(): ConcernPageDto = ConcernPageDto(
        concerns = content,
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

    private fun notFound() = EntityNotFoundException(ConcernErrorCode.CONCERN_NOT_FOUND)
}
