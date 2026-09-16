package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

/** 댓글 신고 접수 기록이다. 동일 사용자·댓글의 중복 신고를 허용한다. */
@Entity
@Table(
    name = "recruitment_post_comment_reports",
    indexes = [
        Index(name = "idx_recruitment_post_comment_report_comment", columnList = "comment_id, created_at"),
        Index(name = "idx_recruitment_post_comment_report_user", columnList = "user_id, created_at"),
    ],
)
internal class RecruitmentPostCommentReport(
    @Column(name = "comment_id", nullable = false)
    val commentId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(length = REASON_MAX_LENGTH)
    val reason: String?,
) : BaseTimeEntity() {

    init {
        require(commentId > 0) { "댓글 식별자는 양수여야 합니다." }
        require(userId > 0) { "신고자 식별자는 양수여야 합니다." }
        require(reason == null || reason.length <= REASON_MAX_LENGTH) { "신고 사유는 500자 이하여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        const val REASON_MAX_LENGTH = 500
    }
}
