package com.ogonggo.core.concern.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/** 취준고민 게시판의 고민글이다. 본문은 에디터 JSON이 아닌 일반 텍스트다. */
@Entity
@Table(
    name = "concerns",
    indexes = [
        Index(name = "idx_concern_category_deleted", columnList = "category, deleted_at"),
        Index(name = "idx_concern_author", columnList = "author_user_id"),
    ],
)
class Concern internal constructor(
    authorUserId: Long,
    category: ConcernCategory,
    title: String,
    content: String,
) : BaseTimeEntity() {

    init {
        require(authorUserId > 0) { "작성자 식별자는 양수여야 합니다." }
        validate(title, content)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "author_user_id", nullable = false)
    val authorUserId: Long = authorUserId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: ConcernCategory = category
        protected set

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    var title: String = title
        protected set

    @Column(nullable = false, length = CONTENT_MAX_LENGTH)
    var content: String = content
        protected set

    /** 운영자가 숨긴 고민글은 사용자에게 없는 글과 같다. 다시 내놓는 것도 운영자만 한다. */
    @Column(nullable = false)
    var hidden: Boolean = false
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
        protected set

    fun isWrittenBy(userId: Long): Boolean = authorUserId == userId

    fun isDeleted(): Boolean = deletedAt != null

    fun update(category: ConcernCategory, title: String, content: String) {
        check(deletedAt == null) { "삭제된 고민글은 수정할 수 없습니다." }
        validate(title, content)
        this.category = category
        this.title = title
        this.content = content
    }

    fun hide() {
        check(deletedAt == null) { "삭제된 고민글은 숨길 수 없습니다." }
        hidden = true
    }

    fun unhide() {
        check(deletedAt == null) { "삭제된 고민글은 다시 노출할 수 없습니다." }
        hidden = false
    }

    fun delete(deletedAt: LocalDateTime) {
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt
        }
    }

    companion object {
        const val TITLE_MAX_LENGTH = 100
        const val CONTENT_MAX_LENGTH = 2000

        private fun validate(title: String, content: String) {
            require(title.isNotBlank()) { "고민글 제목은 공백일 수 없습니다." }
            require(title.length <= TITLE_MAX_LENGTH) { "고민글 제목은 ${TITLE_MAX_LENGTH}자 이하여야 합니다." }
            require(content.isNotBlank()) { "고민글 본문은 공백일 수 없습니다." }
            require(content.length <= CONTENT_MAX_LENGTH) { "고민글 본문은 ${CONTENT_MAX_LENGTH}자 이하여야 합니다." }
        }
    }
}
