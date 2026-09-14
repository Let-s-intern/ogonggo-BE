package com.ogonggo.adminapi.review.business

import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.implement.ContentRejectionManager
import com.ogonggo.core.review.implement.ContentRejectionReader
import com.ogonggo.core.review.implement.dto.ContentRejectionDto
import com.ogonggo.core.review.implement.dto.ContentRejectionPageDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * 반려 기록을 조회하고 사유를 고친다.
 * 고친 사유도 기업회원에게 다시 전달돼야 하지만 전달 경로를 아직 정하지 않아 기록만 바꾼다.
 */
@Service
class AdminRejectionService(
    private val contentRejectionReader: ContentRejectionReader,
    private val contentRejectionManager: ContentRejectionManager,
    private val clock: Clock,
) {

    fun getRejections(
        type: ReviewContentType?,
        keyword: String?,
        page: Int,
        size: Int,
    ): ContentRejectionPageDto = contentRejectionReader.readActivePage(type, keyword, page, size)

    fun getRejection(type: ReviewContentType, contentId: Long): ContentRejectionDto =
        contentRejectionReader.readActive(type, contentId)

    @Transactional
    fun replaceReason(type: ReviewContentType, contentId: Long, reason: String) {
        contentRejectionManager.replaceReason(type, contentId, reason, LocalDateTime.now(clock))
    }
}
