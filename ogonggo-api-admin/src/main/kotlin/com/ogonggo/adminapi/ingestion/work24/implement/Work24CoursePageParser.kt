package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CoursePageDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/**
 * 고용24 훈련과정 상세 화면과 교과편성 화면의 HTML에서 값을 읽는다.
 *
 * 화면은 명세가 없어 고용24가 바꾸면 값을 못 찾는다. 못 찾은 항목은 비워 두고 예외를 내지 않는다.
 * 2026-09-30 실제 화면(K-디지털 트레이닝 3개, 국가기간전략산업직종 2개)으로 구조를 확인했다.
 */
internal object Work24CoursePageParser {

    private val CURRICULUM_PARAMETERS = Regex("""function\s+fn_PopOpen\s*\(\s*\)\s*\{\s*var\s+paramData\s*=\s*\{(.*?)}""", RegexOption.DOT_MATCHES_ALL)
    private val PARAMETER = Regex(""""(\w+)"\s*:\s*(['"])(.*?)\2""")
    private val BLANK = Regex("""[ \t ]+""")

    fun parse(html: String): Document = Jsoup.parse(html)

    /** 훈련과정 개요 표의 제목과 글이다. 제목은 과정마다 다르다. */
    fun overview(page: Document): Map<String, String> =
        page.select("#traCrseinfo tr").mapNotNull { row ->
            val title = row.selectFirst("th")?.text()?.trim().orEmpty()
            val body = row.selectFirst("td")?.let(::lines).orEmpty()
            if (title.isEmpty() || body.isEmpty()) null else title to body
        }.toMap()

    fun instructors(page: Document): List<Work24CoursePageDto.Instructor> =
        rows(page, "훈련강사 표").mapNotNull { cells ->
            val name = cells.getOrNull(0)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Work24CoursePageDto.Instructor(
                name = name,
                major = cells.getOrNull(1)?.takeIf { it.isNotEmpty() },
                qualifications = cells.getOrNull(2)?.takeIf { it.isNotEmpty() },
            )
        }

    /** 교재명만 읽는다. */
    fun textbooks(page: Document): List<String> =
        rows(page, "훈련교재 표").mapNotNull { cells -> cells.firstOrNull()?.takeIf { it.isNotEmpty() } }

    /**
     * 교과편성 화면을 여는 데 필요한 값이다. 상세 화면의 스크립트에 적혀 있고, 빠지면 고용24가 화면을 주지 않는다.
     * 상세 화면의 다른 숨은 입력값에는 담당자 개인 연락처가 있어 이 값만 골라 읽는다.
     */
    fun curriculumParameters(html: String): Map<String, String> =
        CURRICULUM_PARAMETERS.find(html)?.groupValues?.get(1)
            ?.let { block -> PARAMETER.findAll(block).associate { it.groupValues[1] to it.groupValues[3] } }
            .orEmpty()

    /**
     * 교과목명·세부내용·훈련시간 표만 읽는다. NCS를 적용한 과정은 능력단위 표만 있고 세부내용이 없어 빈 목록이다.
     * 능력단위 표에는 화면에서 가려지지 않은 강사 실명이 있어 읽지 않는다.
     */
    fun subjects(curriculumHtml: String): List<Work24CoursePageDto.Subject> =
        Jsoup.parse(curriculumHtml).select("table")
            .filter { table -> table.select("thead th").map { it.text().trim() }.containsAll(SUBJECT_HEADERS) }
            .flatMap { table -> table.select("tbody tr") }
            .mapNotNull { row ->
                val cells = row.select("td")
                val name = cells.getOrNull(0)?.text()?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                Work24CoursePageDto.Subject(
                    name = name,
                    detail = cells.getOrNull(1)?.let(::lines)?.takeIf { it.isNotEmpty() },
                    hours = cells.getOrNull(2)?.text()?.trim()?.takeIf { it.isNotEmpty() },
                )
            }

    /** 표 설명(caption)으로 표를 찾아 본문 행의 칸 글자를 돌려준다. */
    private fun rows(page: Document, captionPrefix: String): List<List<String>> =
        page.select("table")
            .firstOrNull { it.selectFirst("caption")?.text()?.trim()?.startsWith(captionPrefix) == true }
            ?.select("tbody tr")
            ?.map { row -> row.select("td").map { it.text().trim() } }
            .orEmpty()

    /** 줄바꿈을 살려 읽는다. 화면은 줄을 `<br>`과 원문 줄바꿈 두 가지로 나눈다. */
    private fun lines(element: Element): String {
        val copy = element.clone()
        copy.select("br").forEach { it.after(TextNode("\n")) }
        return copy.wholeText().lineSequence()
            .map { it.replace(BLANK, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    private val SUBJECT_HEADERS = listOf("교과목명", "세부내용", "훈련시간")
}
