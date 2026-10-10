package com.ogonggo.core.contentreview.implement

import com.ogonggo.core.paging.validatePageRequest
import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.contentreview.domain.ContentReviewTargetType
import com.ogonggo.core.contentreview.error.ContentReviewErrorCode
import com.ogonggo.core.contentreview.implement.dto.ContentRejectionDto
import com.ogonggo.core.contentreview.implement.dto.ContentRejectionPageDto
import com.ogonggo.core.contentreview.persistence.ContentRejectionQueryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class ContentRejectionReader internal constructor(
    private val contentRejectionQueryRepository: ContentRejectionQueryRepository,
) {

    /** 방금 무엇을 돌려보냈는지 확인하러 오는 목록이므로 최근 반려가 먼저다. */
    fun readActivePage(
        contentType: ContentReviewTargetType?,
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

    fun readActive(contentType: ContentReviewTargetType, contentId: Long): ContentRejectionDto =
        contentRejectionQueryRepository.findActive(contentType, contentId)
            ?: throw EntityNotFoundException(ContentReviewErrorCode.REJECTION_NOT_FOUND)
}
