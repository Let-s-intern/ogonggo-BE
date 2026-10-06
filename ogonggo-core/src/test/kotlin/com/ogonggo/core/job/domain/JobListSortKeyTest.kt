package com.ogonggo.core.job.domain

import com.ogonggo.core.contentreview.domain.ContentSource
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class JobListSortKeyTest {

    @Test
    fun `크롤러 공고는 등록일과 무작위 값에 관계없이 다른 경로 공고보다 앞에 온다`() {
        // given
        val oldCrawled = JobListSortKey.of(ContentSource.CRAWLER, LocalDate.of(2026, 1, 1), Int.MIN_VALUE)

        // when & then
        listOf(ContentSource.COMPANY, ContentSource.WORK24).forEach { source ->
            val newest = JobListSortKey.of(source, LocalDate.of(2026, 12, 31), -1)
            assertTrue(oldCrawled > newest)
        }
    }

    @Test
    fun `같은 경로에서는 늦게 등록한 날의 공고가 무작위 값에 관계없이 앞에 온다`() {
        // given
        val earlier = JobListSortKey.of(ContentSource.CRAWLER, LocalDate.of(2026, 9, 29), -1)
        val later = JobListSortKey.of(ContentSource.CRAWLER, LocalDate.of(2026, 9, 30), 0)

        // when & then
        assertTrue(later > earlier)
    }

    @Test
    fun `같은 날 같은 경로의 공고는 무작위 값으로만 순서가 갈린다`() {
        // given
        val day = LocalDate.of(2026, 9, 30)

        // when
        val keys = listOf(Int.MIN_VALUE, -1, 0, 1, Int.MAX_VALUE).map { JobListSortKey.of(ContentSource.WORK24, day, it) }

        // then
        assertTrue(keys.all { it > 0 })
        assertTrue(keys.toSet().size == keys.size)
        assertTrue(keys.all { it >= JobListSortKey.of(ContentSource.WORK24, day, 0) })
        assertTrue(keys.all { it < JobListSortKey.of(ContentSource.WORK24, day.plusDays(1), 0) })
    }

    @Test
    fun `정렬 키에 담을 수 없는 등록일은 받지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) {
            JobListSortKey.of(ContentSource.CRAWLER, LocalDate.of(1969, 12, 31), 0)
        }
    }
}
