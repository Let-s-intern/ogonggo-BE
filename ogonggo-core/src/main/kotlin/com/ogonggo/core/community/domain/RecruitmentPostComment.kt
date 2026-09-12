package com.ogonggo.core.community.domain

import com.ogonggo.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

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
    post: Post,
    parent: RecruitmentPostComment?,
    userId: Long,
    content: String,
) : BaseTimeEntity() {

    init {
        validateUser(userId)
        validateContent(content)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "post_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_recruitment_post_comment_post"),
    )
    var post: Post = post
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "parent_id",
        foreignKey = ForeignKey(name = "fk_recruitment_post_comment_parent"),
    )
    var parent: RecruitmentPostComment? = parent
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(nullable = false, length = CONTENT_MAX_LENGTH)
    var content: String = content
        protected set

    fun belongsTo(postId: Long): Boolean = post.id == postId

    fun isReply(): Boolean = parent != null

    companion object {
        const val CONTENT_MAX_LENGTH = 1000

        fun create(
            post: Post,
            parent: RecruitmentPostComment?,
            userId: Long,
            content: String,
        ): RecruitmentPostComment = RecruitmentPostComment(
            post = post,
            parent = parent,
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
