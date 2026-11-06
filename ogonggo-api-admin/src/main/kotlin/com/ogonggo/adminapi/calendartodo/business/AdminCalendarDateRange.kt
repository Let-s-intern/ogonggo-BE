package com.ogonggo.adminapi.calendartodo.business

import com.ogonggo.adminapi.error.AdminConsoleBadRequestException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * 관리자 콘솔 캘린더의 조회 기간이다. KST 날짜이고 [from]은 포함, [toExclusive]는 포함하지 않는다.
 * 할 일 목록과 광고 일정 조회가 같은 규칙으로 받는다.
 */
data class AdminCalendarDateRange(
    val from: LocalDate,
    val toExclusive: LocalDate,
) {
    companion object {
        /** 응답 크기가 기간에 비례하므로 둔 상한이다. 달력은 6주(42일)를 보낸다. */
        const val MAX_DAYS = 93L

        private val DATE = Regex("""^\d{4}-\d{2}-\d{2}$""")

        /** 쿼리 문자열을 읽는다. 없거나 형식이 틀리거나 `from >= to`이거나 상한을 넘으면 화면 문구로 알린다. */
        fun parse(from: String?, to: String?): AdminCalendarDateRange {
            val start = dateOf(from)
            val end = dateOf(to)
            if (start == null || end == null || !start.isBefore(end)) {
                throw AdminConsoleBadRequestException("from, to: YYYY-MM-DD(KST) 로, from 이 to 보다 앞서게 보내 주세요.")
            }
            if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
                throw AdminConsoleBadRequestException.field("to", "from 부터 ${MAX_DAYS}일 안으로 보내 주세요.")
            }
            return AdminCalendarDateRange(start, end)
        }

        private fun dateOf(value: String?): LocalDate? {
            if (value == null || !DATE.matches(value)) {
                return null
            }
            return try {
                LocalDate.parse(value)
            } catch (exception: DateTimeParseException) {
                null
            }
        }
    }
}
