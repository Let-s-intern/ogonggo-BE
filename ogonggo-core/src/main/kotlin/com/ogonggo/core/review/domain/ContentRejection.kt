package com.ogonggo.core.review.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/**
 * 운영자가 기업회원 콘텐츠를 반려하며 남긴 사유다.
 *
 * 사유는 콘텐츠의 속성이 아니라 운영자가 올린 사람에게 보낸 말이므로 콘텐츠와 다른 테이블에 둔다.
 * 콘텐츠마다 한 행만 두고, 반려가 풀리면 소프트 삭제했다가 다시 반려하면 그 행을 되살린다.
 * 콘텐츠가 삭제돼도 행은 남겨 "반려하고 지웠다"는 기록을 보존한다.
 */
@Entity
@Table(
    name = "content_rejections",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_content_rejections_content", columnNames = ["content_type", "content_id"]),
    ],
    indexes = [
        Index(name = "idx_content_rejections_active_latest", columnList = "deleted_at, rejected_at"),
    ],
)
class ContentRejection internal constructor(
    contentType: ReviewContentType,
    contentId: Long,
    reason: String,
    rejectedAt: LocalDateTime,
) : BaseTimeEntity() {

    init {
        require(contentId > 0) { "콘텐츠 식별자는 양수여야 합니다." }
        validateReason(reason)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 반려 기록 식별자 */
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    var contentType: ReviewContentType = contentType /* 반려한 콘텐츠 종류 */
        protected set

    @Column(name = "content_id", nullable = false)
    var contentId: Long = contentId /* 반려한 콘텐츠 식별자 */
        protected set

    @Column(nullable = false, length = MAX_REASON_LENGTH)
    var reason: String = reason /* 반려 사유 */
        protected set

    @Column(name = "rejected_at", nullable = false)
    var rejectedAt: LocalDateTime = rejectedAt /* 반려 일시 */
        protected set

    @Column(name = "reason_updated_at")
    var reasonUpdatedAt: LocalDateTime? = null /* 반려 후 사유를 고친 일시 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 반려가 풀린 일시 */
        protected set

    /** 반려가 풀렸던 콘텐츠를 다시 반려한다. 새 판정이므로 반려 일시를 새로 쓰고 사유 수정 일시를 지운다. */
    fun rejectAgain(reason: String, now: LocalDateTime) {
        validateReason(reason)
        this.reason = reason
        rejectedAt = now
        reasonUpdatedAt = null
        deletedAt = null
    }

    fun replaceReason(reason: String, now: LocalDateTime) {
        check(deletedAt == null) { "풀린 반려 기록의 사유는 고칠 수 없습니다." }
        validateReason(reason)
        this.reason = reason
        reasonUpdatedAt = now
    }

    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    companion object {
        const val MAX_REASON_LENGTH = 1000
    }
}

/** 사유 없는 반려는 올린 사람이 무엇을 고칠지 알 수 없어 같은 글이 다시 올라온다. */
private fun validateReason(reason: String) {
    require(reason.isNotBlank()) { "반려 사유는 비어 있을 수 없습니다." }
    require(reason.length <= ContentRejection.MAX_REASON_LENGTH) { "반려 사유가 너무 깁니다." }
}
