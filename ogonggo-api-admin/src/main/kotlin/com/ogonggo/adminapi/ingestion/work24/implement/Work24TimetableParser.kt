package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CoursePageDto
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * 고용24 시간표 엑셀(xlsx)에서 수업 시간을 읽는다.
 *
 * 엑셀은 한 행이 수업 한 칸이고 첫 행이 제목이다(훈련일자, 구분, 시작시간, 종료시간, 적용시간(분), 훈련강사, 교육장소(강의실), 교과목, NCS능력단위).
 * 구분이 `훈련`인 행만 읽고 `점심`은 버린다. 등록된 시간표가 없는 과정은 제목 행만 와서 빈 목록이다.
 *
 * xlsx는 XML을 묶은 zip이라 엑셀 라이브러리 없이 시트와 공유 문자열 두 파일만 읽는다.
 */
internal object Work24TimetableParser {

    fun lessons(xlsx: ByteArray): List<Work24CoursePageDto.Lesson> {
        val entries = entries(xlsx)
        val sheet = entries[SHEET] ?: return emptyList()
        val sharedStrings = entries[SHARED_STRINGS]?.let(::sharedStrings).orEmpty()

        val rows = children(sheet.documentElement, "row").map { row -> cells(row, sharedStrings) }
        val header = rows.firstOrNull() ?: return emptyList()
        val dateColumn = column(header, "훈련일자") ?: return emptyList()
        val typeColumn = column(header, "구분") ?: return emptyList()
        val subjectColumn = column(header, "교과목") ?: return emptyList()

        return rows.drop(1).mapNotNull { row ->
            val date = Work24Values.date(row[dateColumn])
            val subject = row[subjectColumn]?.trim().orEmpty()
            if (row[typeColumn]?.trim() != LESSON || date == null || subject.isEmpty()) {
                null
            } else {
                Work24CoursePageDto.Lesson(date, subject)
            }
        }
    }

    private fun entries(xlsx: ByteArray): Map<String, Document> {
        val documents = mutableMapOf<String, Document>()
        ZipInputStream(ByteArrayInputStream(xlsx)).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                if (entry.name == SHEET || entry.name == SHARED_STRINGS) {
                    // 압축을 푼 크기를 제한해 비정상적으로 큰 파일을 읽지 않는다.
                    val bytes = zip.readNBytes(MAX_ENTRY_BYTES + 1)
                    check(bytes.size <= MAX_ENTRY_BYTES) { "고용24 시간표 파일이 너무 큽니다." }
                    documents[entry.name] = Work24XmlConverter.parse(InputSource(ByteArrayInputStream(bytes)))
                }
            }
        }
        return documents
    }

    private fun sharedStrings(document: Document): List<String> =
        children(document.documentElement, "si").map { it.textContent }

    /** 열 글자(A, B, …)로 칸 값을 찾는다. 빈 칸은 행에 아예 없다. */
    private fun cells(row: Element, sharedStrings: List<String>): Map<String, String> =
        children(row, "c").associate { cell ->
            val column = cell.getAttribute("r").takeWhile { it.isLetter() }
            val value = children(cell, "v").firstOrNull()?.textContent
            column to when {
                value == null -> cell.textContent
                cell.getAttribute("t") == "s" -> sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                else -> value
            }
        }

    private fun column(header: Map<String, String>, title: String): String? =
        header.entries.firstOrNull { it.value.trim() == title }?.key

    private fun children(parent: Element, name: String): List<Element> {
        val nodes = parent.getElementsByTagName(name)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private const val SHEET = "xl/worksheets/sheet1.xml"
    private const val SHARED_STRINGS = "xl/sharedStrings.xml"
    private const val LESSON = "훈련"
    private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024
}
