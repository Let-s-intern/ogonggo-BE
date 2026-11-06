package com.ogonggo.adminapi.calendartodo.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import com.ogonggo.core.calendartodo.domain.CalendarTodo
import com.ogonggo.core.calendartodo.domain.CalendarTodoTerms
import java.time.LocalDate
import java.util.Optional

/** 할 일 등록 본문이다. 검증 전 값을 그대로 담는다. 내용을 빼면 null로 본다. */
data class AdminCalendarTodoCreateCommand(
    val title: String?,
    val content: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val done: Boolean?,
) {
    internal fun toTerms(): CalendarTodoTerms =
        validTerms(title = title, content = content, startDate = startDate, endDate = endDate, done = done)
}

/**
 * 할 일 수정 본문이다. null이면 저장된 값을 그대로 둔다. `content`는 보내지 않으면 null, null을 보내면 빈 [Optional]이라
 * 내용을 지운다.
 */
data class AdminCalendarTodoUpdateCommand(
    val title: String? = null,
    val content: Optional<String>? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val done: Boolean? = null,
) {
    /** 저장된 값과 합친 결과로 검증한다. */
    internal fun mergeInto(todo: CalendarTodo): CalendarTodoTerms = validTerms(
        title = title ?: todo.title,
        content = if (content == null) todo.content else content.orElse(null),
        startDate = startDate ?: todo.startDate,
        endDate = endDate ?: todo.endDate,
        done = done ?: todo.done,
    )
}

/** 프런트 목과 같은 순서(title → content → startDate → endDate → 앞뒤 → done)로 보고 처음 걸린 칸 하나만 알린다. */
private fun validTerms(
    title: String?,
    content: String?,
    startDate: LocalDate?,
    endDate: LocalDate?,
    done: Boolean?,
): CalendarTodoTerms {
    val trimmedTitle = title?.trim().orEmpty()
    if (trimmedTitle.isEmpty() || trimmedTitle.length > CalendarTodoTerms.MAX_TITLE_LENGTH) {
        throw AdminConsoleBadRequestException.field("title", "1~${CalendarTodoTerms.MAX_TITLE_LENGTH}자로 보내 주세요.")
    }
    if ((content?.trim()?.length ?: 0) > CalendarTodoTerms.MAX_CONTENT_LENGTH) {
        throw AdminConsoleBadRequestException.field("content", "null 이거나 ${CalendarTodoTerms.MAX_CONTENT_LENGTH}자까지 보낼 수 있습니다.")
    }
    val start = startDate ?: throw AdminConsoleBadRequestException.field("startDate", "YYYY-MM-DD(KST) 로 보내 주세요.")
    val end = endDate ?: throw AdminConsoleBadRequestException.field("endDate", "YYYY-MM-DD(KST) 로 보내 주세요.")
    if (end.isBefore(start)) {
        throw AdminConsoleBadRequestException.field("endDate", "끝나는 날은 시작일과 같거나 뒤여야 합니다.")
    }
    val checked = done ?: throw AdminConsoleBadRequestException.field("done", "참·거짓 값을 보내 주세요.")
    return CalendarTodoTerms(title = trimmedTitle, content = content, startDate = start, endDate = end, done = checked)
}
