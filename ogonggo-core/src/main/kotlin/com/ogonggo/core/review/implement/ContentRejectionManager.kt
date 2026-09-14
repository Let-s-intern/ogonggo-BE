package com.ogonggo.core.review.implement

import com.ogonggo.core.error.EntityNotFoundException
import com.ogonggo.core.review.domain.ContentRejection
import com.ogonggo.core.review.domain.ReviewContentType
import com.ogonggo.core.review.error.ReviewErrorCode
import com.ogonggo.core.review.persistence.ContentRejectionJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component

/**
 * 반려 기록을 남기고 푼다.
 * 콘텐츠의 검수 상태를 바꾸는 쪽이 콘텐츠 행을 잠근 뒤 호출하므로 같은 콘텐츠의 기록은 순서대로 바뀐다.
 */
@Component
class ContentRejectionManager internal constructor(
    private val contentRejectionRepository: ContentRejectionJpaRepository,
) {

    /** 같은 콘텐츠를 다시 반려하면 새 행을 만들지 않고 기존 행에 새 사유를 쓴다. */
    fun reject(contentType: ReviewContentType, contentId: Long, reason: String, now: LocalDateTime) {
        val existing = contentRejectionRepository.findByContentTypeAndContentId(contentType, contentId)
        if (existing == null) {
            contentRejectionRepository.save(ContentRejection(contentType, contentId, reason, now))
            return
        }
        existing.rejectAgain(reason, now)
        contentRejectionRepository.save(existing)
    }

    /** 반려가 풀리면 기록을 지운다. 남겨 두면 반려 보관에 승인된 건이 섞인다. */
    fun clear(contentType: ReviewContentType, contentId: Long, now: LocalDateTime) {
        val active = contentRejectionRepository.findByContentTypeAndContentIdAndDeletedAtIsNull(contentType, contentId)
            ?: return
        active.delete(now)
        contentRejectionRepository.save(active)
    }

    fun replaceReason(contentType: ReviewContentType, contentId: Long, reason: String, now: LocalDateTime) {
        val active = contentRejectionRepository.findByContentTypeAndContentIdAndDeletedAtIsNull(contentType, contentId)
            ?: throw EntityNotFoundException(ReviewErrorCode.REJECTION_NOT_FOUND)
        active.replaceReason(reason, now)
        contentRejectionRepository.save(active)
    }
}
