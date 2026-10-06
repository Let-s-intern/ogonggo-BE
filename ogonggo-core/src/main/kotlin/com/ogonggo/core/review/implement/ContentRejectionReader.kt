package com.ogonggo.core.review.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.error.ReviewErrorCode
import com.ogonggo.core.review.implement.dto.ContentRejectionDto
import com.ogonggo.core.review.implement.dto.ContentRejectionPageDto
import com.ogonggo.core.review.persistence.ContentRejectionQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ContentRejectionReader internal constructor(
    private val contentRejectionQueryRepository: ContentRejectionQueryRepository,
) {

    /** 방금 무엇을 돌려보냈는지 확인하러 오는 목록이므로 최근 반려가 먼저다. */
    fun readActivePage(
        contentType: ReviewContentType?,
        keyword: String?,
        page: Int,
        size: Int,
    ): ContentRejectionPageDto {
        validatePageRequest(page, size)
        val result = contentRejectionQueryRepository.findActivePage(contentType, keyword, PageRequest.of(page, size))
        return ContentRejectionPageDto(
            rejections = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
        )
    }

    fun readActive(contentType: ReviewContentType, contentId: Long): ContentRejectionDto =
        contentRejectionQueryRepository.findActive(contentType, contentId)
            ?: throw EntityNotFoundException(ReviewErrorCode.REJECTION_NOT_FOUND)
}
