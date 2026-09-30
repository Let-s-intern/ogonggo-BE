package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24WorkStudyPageDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * 고용24 일학습병행 훈련과정 상세 화면(`selectJobAndStudyTracseDetail.do`)의 HTML에서 값을 읽는다.
 *
 * 화면은 명세가 없어 고용24가 바꾸면 값을 못 찾는다. 못 찾은 항목은 비워 둔다.
 * 2026-09-30 실제 화면(공동훈련센터형 5개, 도제학교 1개)으로 구조를 확인했다.
 */
internal object Work24WorkStudyPageParser {

    fun parse(html: String): Work24WorkStudyPageDto {
        val page = Jsoup.parse(html)
        val ojt = page.selectFirst("#section_ojt")
        val ojtRows = ojt?.let(::rows).orEmpty()

        return Work24WorkStudyPageDto(
            company = page.selectFirst("p.corp_info strong")?.text()?.meaningful(),
            purpose = ojtRows["훈련목적"],
            mainContent = ojtRows["주요 훈련 내용"],
            requirement = ojtRows["훈련 대상 요건"],
            ojtHours = ojtRows[HOURS],
            offJtHours = page.selectFirst("#section_offjt")?.let(::rows)?.get(HOURS),
            ojtSubjects = ojt?.let(::subjects).orEmpty(),
        )
    }

    /** 훈련정보·훈련내용 표는 한 행에 제목과 값이 한 쌍 또는 두 쌍 있다. 값이 없는 칸은 뺀다. */
    private fun rows(section: Element): Map<String, String> =
        section.select("tr").flatMap { row ->
            row.select("th").mapNotNull { title ->
                val value = title.nextElementSibling()?.takeIf { it.tagName() == "td" }?.text()?.meaningful()
                value?.let { title.text().trim() to it }
            }
        }.toMap()

    private fun subjects(section: Element): List<Work24WorkStudyPageDto.Subject> =
        section.select("table")
            .filter { table -> table.selectFirst("caption")?.text()?.trim() == SUBJECT_TABLE }
            .flatMap { table -> table.select("tbody tr") }
            .mapNotNull { row ->
                val cells = row.select("td").map { it.text().meaningful() }
                // 편성이 없으면 `자료가 없습니다` 한 칸짜리 행이 온다.
                val name = cells.getOrNull(0)?.takeIf { cells.size >= SUBJECT_COLUMNS } ?: return@mapNotNull null
                Work24WorkStudyPageDto.Subject(
                    name = name,
                    unit = cells.getOrNull(1),
                    required = cells.getOrNull(3),
                    hours = cells.getOrNull(4),
                )
            }

    /**
     * 비었거나 뜻이 없는 값은 null이다. 고용24는 빈 칸을 `-`로 채우고,
     * 확인한 과정은 모두 주요 훈련 내용과 훈련 대상 요건에 같은 안내 문구만 적혀 있었다.
     */
    private fun String.meaningful(): String? =
        trim().takeIf { it.isNotEmpty() && it != EMPTY && PLACEHOLDER !in it }

    private const val HOURS = "훈련시간"
    private const val SUBJECT_TABLE = "훈련 편성 정보"
    private const val SUBJECT_COLUMNS = 5
    private const val EMPTY = "-"
    private const val PLACEHOLDER = "PDMS 시스템 개발보고서 참고"
}
