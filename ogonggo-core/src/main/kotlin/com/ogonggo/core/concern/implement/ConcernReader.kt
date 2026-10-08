package com.ogonggo.core.concern.implement

import com.ogonggo.core.concern.domain.Concern
import com.ogonggo.core.concern.domain.ConcernCategory
import com.ogonggo.core.concern.domain.ConcernSortType
import com.ogonggo.core.concern.error.ConcernErrorCode
import com.ogonggo.core.concern.implement.dto.ConcernPageDto
import com.ogonggo.core.concern.persistence.ConcernJpaRepository
import com.ogonggo.core.concern.persistence.ConcernQueryRepository
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.paging.validatePageRequest
import org.springframework.stereotype.Component

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

    private fun notFound() = EntityNotFoundException(ConcernErrorCode.CONCERN_NOT_FOUND)
}
