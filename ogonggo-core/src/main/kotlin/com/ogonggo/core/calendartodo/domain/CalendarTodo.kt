package com.ogonggo.core.calendartodo.domain

import com.ogonggo.core.jpa.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 운영자가 관리자 콘솔 캘린더에 적는 할 일이다. 운영자 모두가 같은 목록을 본다(쓴 사람을 두지 않는다).
 *
 * 시각 없이 날짜만 두고 시작일·끝나는 날을 모두 포함한다. 하루짜리는 둘이 같다. 완료는 운영자가 켜고 끄는 값이다.
 */
@Entity
@Table(
    name = "calendar_todos",
    indexes = [
        Index(name = "idx_calendar_todos_start_date", columnList = "deleted_at, start_date"),
    ],
)
class CalendarTodo internal constructor(
    terms: CalendarTodoTerms,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null /* 할 일 식별자 */
        protected set

    @Column(nullable = false, length = CalendarTodoTerms.MAX_TITLE_LENGTH)
    var title: String = terms.title /* 제목 */
        protected set

    @Column(length = CalendarTodoTerms.MAX_CONTENT_LENGTH)
    var content: String? = terms.content /* 내용. 여러 줄 */
        protected set

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = terms.startDate /* 시작일(포함) */
        protected set

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate = terms.endDate /* 끝나는 날(포함) */
        protected set

    @Column(nullable = false)
    var done: Boolean = terms.done /* 완료 여부 */
        protected set

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null /* 삭제 일시 */
        protected set

    val terms: CalendarTodoTerms
        get() = CalendarTodoTerms(title = title, content = content, startDate = startDate, endDate = endDate, done = done)

    /** 내용을 통째로 바꾼다. 부분 수정은 저장된 값과 합친 [terms]를 넘긴다. */
    fun change(terms: CalendarTodoTerms) {
        check(deletedAt == null) { "삭제된 할 일은 수정할 수 없습니다." }
        title = terms.title
        content = terms.content
        startDate = terms.startDate
        endDate = terms.endDate
        done = terms.done
    }

    /** 삭제는 반복해도 최초 삭제 일시를 유지한다. */
    fun delete(now: LocalDateTime) {
        if (deletedAt == null) {
            deletedAt = now
        }
    }
}

/** 할 일 내용이다. 제목·내용은 앞뒤 공백을 빼고, 빈 내용은 null로 맞춘다. */
class CalendarTodoTerms(
    title: String,
    content: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val done: Boolean,
) {
    val title: String = title.trim()
    val content: String? = content?.trim()?.ifEmpty { null }

    init {
        require(this.title.isNotEmpty() && this.title.length <= MAX_TITLE_LENGTH) {
            "할 일 제목은 1자 이상 ${MAX_TITLE_LENGTH}자 이하여야 합니다."
        }
        require((this.content?.length ?: 0) <= MAX_CONTENT_LENGTH) { "할 일 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다." }
        require(!endDate.isBefore(startDate)) { "끝나는 날은 시작일과 같거나 뒤여야 합니다." }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 100
        const val MAX_CONTENT_LENGTH = 2000
    }
}
