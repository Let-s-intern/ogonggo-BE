package com.ogonggo.core.concern.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

/** 댓글의 좋아요 표시다. 사용자와 댓글마다 한 행을 두고, 취소했다 다시 누르면 행을 복구한다. */
@Entity
@Table(
    name = "concern_comment_likes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_concern_comment_like_comment_user", columnNames = ["comment_id", "user_id"]),
    ],
)
internal class ConcernCommentLike(
    @Column(name = "comment_id", nullable = false)
    val commentId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : BaseTimeEntity() {

    init {
        require(commentId > 0) { "댓글 식별자는 양수여야 합니다." }
        require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set
}
