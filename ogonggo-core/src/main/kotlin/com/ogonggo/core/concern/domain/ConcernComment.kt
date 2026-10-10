package com.ogonggo.core.concern.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 고민글의 댓글이다. 부모 댓글이 화면의 답변이고, 1단계 대댓글이 답변에 단 답글이다.
 * [official]은 작성 시점에 관리자였는지를 남긴 값이라 나중에 역할이 바뀌어도 그대로 둔다.
 */
@Entity
@Table(
    name = "concern_comments",
    indexes = [
        Index(name = "idx_concern_comment_concern_parent_created", columnList = "concern_id, parent_id, created_at, id"),
    ],
)
class ConcernComment internal constructor(
    concernId: Long,
    parentId: Long?,
    userId: Long,
    content: String,
    official: Boolean,
) : BaseTimeEntity() {

    init {
        require(concernId > 0) { "고민글 식별자는 양수여야 합니다." }
        require(parentId == null || parentId > 0) { "부모 댓글 식별자는 양수여야 합니다." }
        require(userId > 0) { "댓글 작성자 식별자는 양수여야 합니다." }
        require(content.isNotBlank()) { "댓글 내용은 공백일 수 없습니다." }
        require(content.length <= CONTENT_MAX_LENGTH) { "댓글 내용은 ${CONTENT_MAX_LENGTH}자 이하여야 합니다." }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "concern_id", nullable = false)
    val concernId: Long = concernId

    @Column(name = "parent_id")
    val parentId: Long? = parentId

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(nullable = false, length = CONTENT_MAX_LENGTH)
    val content: String = content

    @Column(nullable = false)
    val official: Boolean = official

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun isReply(): Boolean = parentId != null

    fun isWrittenBy(userId: Long): Boolean = this.userId == userId

    fun delete(deletedAt: LocalDateTime) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt
        }
    }

    companion object {
        const val CONTENT_MAX_LENGTH = 1000
    }
}
