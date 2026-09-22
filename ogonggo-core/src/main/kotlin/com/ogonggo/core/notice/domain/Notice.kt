package com.ogonggo.core.notice.domain

import com.ogonggo.core.common.BaseTimeEntity
import com.ogonggo.core.editor.lexical.LexicalEditorStateJson
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 운영자가 관리자 콘솔에서 올리는 공지다. 작성·수정·삭제는 관리자만 하고 사용자는 노출된 공지를 읽기만 한다.
 *
 * 노출 여부는 운영자가 사용자에게 내놓았는가 하나뿐이라 채용공고처럼 게시 상태 네 값을 두지 않는다.
 * 상단 고정은 노출과 별개다. 비노출 공지를 고정해 두면 다시 노출할 때 고정된 채로 나온다.
 */
@Entity
@Table(
    name = "notices",
    indexes = [
        Index(name = "idx_notices_public", columnList = "deleted_at, published, pinned, id"),
    ],
)
class Notice internal constructor(
    title: String,
    content: String,
    pinned: Boolean,
    published: Boolean,
) : BaseTimeEntity() {

    init {
        validateTitle(title)
        validateContent(content)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 공지 식별자 */
        protected set

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    var title: String = title /* 제목 */
        protected set

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    var content: String = content /* Lexical EditorState JSON */
        protected set

    @Column(nullable = false)
    var pinned: Boolean = pinned /* 목록 상단 고정 여부 */
        protected set

    @Column(nullable = false)
    var published: Boolean = published /* 사용자 노출 여부 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    fun edit(title: String?, content: String?) {
        checkNotDeleted()
        title?.let(::validateTitle)
        content?.let(::validateContent)
        title?.let { this.title = it }
        content?.let { this.content = it }
    }

    fun pin(pinned: Boolean) {
        checkNotDeleted()
        this.pinned = pinned
    }

    fun publish() {
        checkNotDeleted()
        published = true
    }

    fun hide() {
        checkNotDeleted()
        published = false
    }

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    private fun checkNotDeleted() {
        check(deletedAt == null) { "삭제된 공지는 수정할 수 없습니다." }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 255
    }
}

private fun validateTitle(title: String) {
    require(title.isNotBlank() && title.length <= Notice.MAX_TITLE_LENGTH) {
        "공지 제목은 1자 이상 ${Notice.MAX_TITLE_LENGTH}자 이하여야 합니다."
    }
}

private fun validateContent(content: String) {
    require(LexicalEditorStateJson.isValid(content)) { "공지 본문은 올바른 에디터 JSON이어야 합니다." }
}
