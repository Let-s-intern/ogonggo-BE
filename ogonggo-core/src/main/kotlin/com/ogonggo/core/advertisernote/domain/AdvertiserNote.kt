package com.ogonggo.core.advertisernote.domain

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
 * 운영자가 광고주마다 여러 건 남기는 메모다. 광고 계약 상세에서 쓰고 읽는다.
 *
 * 쓴 사람은 관리자 토큰의 사용자로 정하고 본문으로 받지 않는다. 쓴 사람을 모르는 메모(옮겨 온 메모 등)는 작성자가 없다.
 */
@Entity
@Table(
    name = "advertiser_notes",
    indexes = [
        Index(name = "idx_advertiser_notes_advertiser_id", columnList = "advertiser_id, deleted_at"),
    ],
)
class AdvertiserNote internal constructor(
    advertiserId: Long,
    title: String,
    content: String,
    authorUserId: Long?,
) : BaseTimeEntity() {

    init {
        require(advertiserId > 0) { "광고주 식별자는 양수여야 합니다." }
        require(authorUserId == null || authorUserId > 0) { "작성자 식별자는 양수여야 합니다." }
        validateTitle(title)
        validateContent(content)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 광고주 메모 식별자 */
        protected set

    @Column(name = "advertiser_id", nullable = false)
    val advertiserId: Long = advertiserId /* 광고주 식별자 */

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    var title: String = title /* 제목 */
        protected set

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    var content: String = content /* 내용. 여러 줄 */
        protected set

    @Column(name = "author_user_id")
    val authorUserId: Long? = authorUserId /* 쓴 관리자의 사용자 식별자 */

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    /** 넘어온 칸만 바꾼다. */
    fun edit(title: String?, content: String?) {
        check(deletedAt == null) { "삭제된 메모는 수정할 수 없습니다." }
        title?.let(::validateTitle)
        content?.let(::validateContent)
        title?.let { this.title = it }
        content?.let { this.content = it }
    }

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 100
        const val MAX_CONTENT_LENGTH = 2000
    }
}

private fun validateTitle(title: String) {
    require(title.isNotBlank() && title.length <= AdvertiserNote.MAX_TITLE_LENGTH) {
        "메모 제목은 1자 이상 ${AdvertiserNote.MAX_TITLE_LENGTH}자 이하여야 합니다."
    }
}

private fun validateContent(content: String) {
    require(content.isNotBlank() && content.length <= AdvertiserNote.MAX_CONTENT_LENGTH) {
        "메모 내용은 1자 이상 ${AdvertiserNote.MAX_CONTENT_LENGTH}자 이하여야 합니다."
    }
}
