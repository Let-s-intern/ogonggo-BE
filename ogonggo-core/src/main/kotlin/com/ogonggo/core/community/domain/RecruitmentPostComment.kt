package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "recruitment_post_comments",
    indexes = [
        Index(
            name = "idx_recruitment_post_comment_post_parent_created_id",
            columnList = "post_id, parent_id, created_at, id",
        ),
    ],
)
class RecruitmentPostComment internal constructor(
    postId: Long,
    parentId: Long?,
    userId: Long,
    content: String,
) : BaseTimeEntity() {

    init {
        require(postId > 0) { "모집글 식별자는 양수여야 합니다." }
        validateUser(userId)
        validateContent(content)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "post_id", nullable = false)
    val postId: Long = postId

    @Column(name = "parent_id")
    val parentId: Long? = parentId

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(nullable = false, length = CONTENT_MAX_LENGTH)
    var content: String = content
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun belongsTo(postId: Long): Boolean = this.postId == postId

    fun isReply(): Boolean = parentId != null

    fun delete(deletedAt: LocalDateTime) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt
        }
    }

    companion object {
        const val CONTENT_MAX_LENGTH = 1000

        fun create(
            postId: Long,
            parentId: Long?,
            userId: Long,
            content: String,
        ): RecruitmentPostComment = RecruitmentPostComment(
            postId = postId,
            parentId = parentId,
            userId = userId,
            content = content,
        )

        private fun validateUser(userId: Long) {
            require(userId > 0) { "댓글 작성자 식별자는 양수여야 합니다." }
        }

        private fun validateContent(content: String) {
            require(content.isNotBlank()) { "댓글 내용은 공백일 수 없습니다." }
            require(content.length <= CONTENT_MAX_LENGTH) { "댓글 내용은 1000자 이하여야 합니다." }
        }
    }
}
