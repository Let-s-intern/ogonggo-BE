package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernPopularSortType
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.concern.error.ConcernErrorCode
import com.ogonggo.core.concern.implement.dto.ConcernPageDto
import com.ogonggo.core.concern.persistence.ConcernJpaRepository
import com.ogonggo.core.concern.persistence.ConcernQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.paging.validatePageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ConcernReader internal constructor(
    private val concernRepository: ConcernJpaRepository,
    private val concernQueryRepository: ConcernQueryRepository,
) {

    fun read(concernId: Long): Concern =
        concernRepository.findByIdAndDeletedAtIsNull(concernId) ?: throw notFound()

    fun readForUpdate(concernId: Long): Concern =
        concernRepository.findActiveByIdForUpdate(concernId) ?: throw notFound()

    /** 삭제된 고민글도 읽는다. 삭제 요청을 반복해도 같은 결과를 주는 데만 쓴다. */
    fun readIncludingDeletedForUpdate(concernId: Long): Concern =
        concernRepository.findByIdForUpdate(concernId) ?: throw notFound()

    fun readPage(
        category: ConcernCategory?,
        sortType: ConcernSortType,
        page: Int,
        size: Int,
    ): ConcernPageDto {
        validatePageRequest(page, size)
        val result = concernQueryRepository.findPage(category, sortType, page, size)
        return ConcernPageDto(
            concerns = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    /** [createdFrom] 이후에 등록한 고민글 중 조회 수나 답변 수가 많은 것을 [limit]건까지 읽는다. */
    fun readPopular(createdFrom: LocalDateTime, sortType: ConcernPopularSortType, limit: Int): List<Concern> {
        require(limit in 1..100) { "인기 고민글 개수는 1 이상 100 이하여야 합니다." }
        return concernQueryRepository.findPopular(createdFrom, sortType, limit)
    }

    private fun notFound() = EntityNotFoundException(ConcernErrorCode.CONCERN_NOT_FOUND)
}
