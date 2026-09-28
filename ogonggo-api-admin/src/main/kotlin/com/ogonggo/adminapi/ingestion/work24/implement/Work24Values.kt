package com.ogonggo.adminapi.ingestion.work24.implement

import com.fasterxml.jackson.databind.JsonNode
import java.time.DateTimeException
import java.time.LocalDate

/** 고용24 응답 값을 오공고 칸에 옮길 때 쓰는 공통 변환이다. 고용24는 모든 값을 문자열로 준다. */
internal object Work24Values {

    private val DATE_PATTERN = Regex("""(\d{2,4})[-./](\d{1,2})[-./](\d{1,2})""")
    private val COMPACT_DATE_PATTERN = Regex("""(?<!\d)(\d{4})(\d{2})(\d{2})(?!\d)""")
    private val NUMBER_PATTERN = Regex("""\d[\d,]*""")

    /** 비었거나 없는 항목은 null이다. */
    fun JsonNode.text(field: String): String? =
        path(field).takeIf { it.isValueNode }?.asText()?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * `2026-10-31`, `26-10-31`, `2026.10.31`, `20261031`을 읽는다. 앞뒤에 `채용시까지` 같은 글자가 붙어도 된다.
     * 두 자리 연도는 2000년대로 본다.
     */
    fun date(value: String?): LocalDate? {
        if (value == null) {
            return null
        }
        val (year, month, day) = (DATE_PATTERN.find(value) ?: COMPACT_DATE_PATTERN.find(value))
            ?.destructured
            ?: return null
        return try {
            LocalDate.of(year.toInt().let { if (it < 100) it + 2000 else it }, month.toInt(), day.toInt())
        } catch (exception: DateTimeException) {
            null
        }
    }

    /** `1,200,000원`, `00명`처럼 글자가 섞인 값에서 첫 숫자를 읽는다. */
    fun number(value: String?): Long? =
        value?.let { NUMBER_PATTERN.find(it) }?.value?.replace(",", "")?.toLongOrNull()

    /** DB 칸 길이를 넘는 값은 잘라 넣는다. 원문 전체는 원문 링크로 볼 수 있다. */
    fun String.limit(maxLength: Int): String = if (length <= maxLength) this else take(maxLength)

    /** `라벨: 값` 줄로 본문을 만든다. 값이 하나도 없으면 null이다. */
    fun section(vararg lines: Pair<String, String?>): String? =
        lines.filter { !it.second.isNullOrBlank() }
            .joinToString("\n") { (label, value) -> "$label: $value" }
            .ifBlank { null }
}
