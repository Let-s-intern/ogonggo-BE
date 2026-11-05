package com.ogonggo.core.job.domain

import com.ogonggo.core.contentreview.domain.ContentSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JobListSortKeyTest {

    @Test
    fun `크롤러 공고와 기업회원 공고는 등록일과 무작위 값에 관계없이 고용24 공고보다 앞에 온다`() {
        // given
        val newestWork24 = JobListSortKey.of(ContentSource.WORK24, LocalDate.of(2026, 12, 31), false, -1)

        // when & then
        listOf(ContentSource.CRAWLER, ContentSource.COMPANY).forEach { source ->
            val oldest = JobListSortKey.of(source, LocalDate.of(2026, 1, 1), false, Int.MIN_VALUE)
            assertTrue(oldest > newestWork24)
        }
    }

    @Test
    fun `기업회원 공고는 크롤러 공고와 등록일 기준으로 섞인다`() {
        // given
        val day = LocalDate.of(2026, 9, 30)
        val crawledThatDay = JobListSortKey.of(ContentSource.CRAWLER, day, false, 0)
        val company = JobListSortKey.of(ContentSource.COMPANY, day, false, 0)

        // when & then
        assertEquals(crawledThatDay, company)
        assertTrue(company > JobListSortKey.of(ContentSource.CRAWLER, day.minusDays(1), true, -1))
        assertTrue(company < JobListSortKey.of(ContentSource.CRAWLER, day.plusDays(1), false, 0))
    }

    @Test
    fun `같은 경로에서는 늦게 등록한 날의 공고가 무작위 값에 관계없이 앞에 온다`() {
        // given
        val earlier = JobListSortKey.of(ContentSource.CRAWLER, LocalDate.of(2026, 9, 29), false, -1)
        val later = JobListSortKey.of(ContentSource.CRAWLER, LocalDate.of(2026, 9, 30), false, 0)

        // when & then
        assertTrue(later > earlier)
    }

    @Test
    fun `같은 날 같은 경로의 공고는 무작위 값으로만 순서가 갈린다`() {
        // given
        val day = LocalDate.of(2026, 9, 30)

        // when
        val keys = listOf(0, 1, 2, 1 shl 30, Int.MAX_VALUE).map { JobListSortKey.of(ContentSource.WORK24, day, false, it) }

        // then
        assertTrue(keys.all { it > 0 })
        assertTrue(keys.toSet().size == keys.size)
        assertTrue(keys.all { it >= JobListSortKey.of(ContentSource.WORK24, day, false, 0) })
        assertTrue(keys.all { it < JobListSortKey.of(ContentSource.WORK24, day.plusDays(1), false, 0) })
    }

    @Test
    fun `같은 날 같은 경로에서는 PICK 직군 공고가 무작위 값에 관계없이 앞에 오고 다음 날 공고는 앞지르지 않는다`() {
        // given
        val day = LocalDate.of(2026, 9, 30)
        val pick = JobListSortKey.of(ContentSource.CRAWLER, day, true, 0)
        val other = JobListSortKey.of(ContentSource.CRAWLER, day, false, Int.MAX_VALUE)
        val nextDay = JobListSortKey.of(ContentSource.CRAWLER, day.plusDays(1), false, 0)

        // when & then
        assertTrue(pick > other)
        assertTrue(nextDay > JobListSortKey.of(ContentSource.CRAWLER, day, true, -1))
        assertEquals(JobListSortKey.PICK_FIELD_BIT, pick and JobListSortKey.PICK_FIELD_BIT)
        assertEquals(0L, other and JobListSortKey.PICK_FIELD_BIT)
    }

    @Test
    fun `정렬 키에 담을 수 없는 등록일은 받지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) {
            JobListSortKey.of(ContentSource.CRAWLER, LocalDate.of(1969, 12, 31), false, 0)
        }
    }
}
