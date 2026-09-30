package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CoursePageDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Work24TimetableParserTest {

    @Test
    fun `시간표 엑셀에서 훈련 행의 날짜와 교과목만 읽고 점심은 버린다`() {
        val xlsx = work24TimetableXlsx(
            listOf("2026-09-30", "훈련", "09:00", "10:00", "60.0", "이*철", "501강의실", "(비NCS)입학 OT", ""),
            listOf("2026-09-30", "점심", "13:00", "14:00", "60.0", "", "-", "", ""),
            listOf("2026-10-01", "훈련", "09:00", "10:00", "60.0", "이*철", "501강의실", "(비NCS)자바", ""),
        )

        val lessons = Work24TimetableParser.lessons(xlsx)

        assertEquals(
            listOf(
                Work24CoursePageDto.Lesson(LocalDate.of(2026, 9, 30), "(비NCS)입학 OT"),
                Work24CoursePageDto.Lesson(LocalDate.of(2026, 10, 1), "(비NCS)자바"),
            ),
            lessons,
        )
    }

    @Test
    fun `등록된 시간표가 없어 제목 행만 오면 빈 목록이다`() {
        assertTrue(Work24TimetableParser.lessons(work24TimetableXlsx()).isEmpty())
    }
}

/**
 * 고용24 시간표 엑셀과 같은 모양의 xlsx를 만든다. 실제 파일처럼 글자는 공유 문자열로, 빈 칸은 칸 없이 둔다.
 */
internal fun work24TimetableXlsx(vararg rows: List<String>): ByteArray {
    val header = listOf("훈련일자", "구분", "시작시간", "종료시간", "적용시간(분)", "훈련강사", "교육장소(강의실)", "교과목", "NCS능력단위")
    val sharedStrings = mutableListOf<String>()
    val sheetRows = (listOf(header) + rows).mapIndexed { rowIndex, cells ->
        val columns = cells.mapIndexedNotNull { columnIndex, value ->
            if (value.isEmpty()) {
                null
            } else {
                val index = sharedStrings.indexOf(value).takeIf { it >= 0 } ?: sharedStrings.size.also { sharedStrings += value }
                """<c r="${'A' + columnIndex}${rowIndex + 1}" t="s"><v>$index</v></c>"""
            }
        }
        """<row r="${rowIndex + 1}">${columns.joinToString("")}</row>"""
    }
    val namespace = """xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main""""
    val files = mapOf(
        "xl/sharedStrings.xml" to "<sst $namespace>${sharedStrings.joinToString("") { "<si><t>$it</t></si>" }}</sst>",
        "xl/worksheets/sheet1.xml" to "<worksheet $namespace><sheetData>${sheetRows.joinToString("")}</sheetData></worksheet>",
    )

    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
        files.forEach { (name, xml) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(xml.toByteArray())
            zip.closeEntry()
        }
    }
    return bytes.toByteArray()
}
