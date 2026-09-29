package com.ogonggo.adminapi.ingestion.work24.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Work24ValuesTest {

    @Test
    fun `고용24가 쓰는 여러 날짜 모양을 읽고 날짜가 없으면 null이다`() {
        val expected = LocalDate.of(2026, 10, 31)

        listOf("2026-10-31", "26-10-31", "2026.10.31", "20261031", "채용시까지 26-10-31").forEach {
            assertEquals(expected, Work24Values.date(it), it)
        }
        assertNull(Work24Values.date("채용시까지"))
        assertNull(Work24Values.date("2026-13-40"))
    }

    @Test
    fun `글자가 섞인 금액과 인원에서 숫자를 읽는다`() {
        assertEquals(1_200_000L, Work24Values.number("1,200,000원"))
        assertEquals(3L, Work24Values.number("3명"))
        assertNull(Work24Values.number("미정"))
    }

    @Test
    fun `고용24가 HTML 엔티티로 담아 온 제목을 풀어 읽는다`() {
        // XML에는 &amp;lt;처럼 한 번 더 이스케이프되어 온다.
        val item = Work24XmlConverter.toJson("<wanted><title>영업사원 &amp;lt;건축자재&amp;gt; 생산&amp;amp;제조</title></wanted>")

        with(Work24Values) {
            assertEquals("영업사원 <건축자재> 생산&제조", item.text("title"))
        }
    }
}
