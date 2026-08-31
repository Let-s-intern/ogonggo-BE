package com.ogonggo.core.bootcamp.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BootcampMetricTest {

    @Test
    fun `조회 북마크 댓글 수를 증감한다`() {
        val metric = BootcampMetric(bootcampId = 1L)

        metric.increaseViewCount()
        metric.increaseBookmarkCount()
        metric.increaseCommentCount()
        metric.decreaseViewCount()
        metric.decreaseBookmarkCount()
        metric.decreaseCommentCount()

        assertEquals(0, metric.viewCount)
        assertEquals(0, metric.bookmarkCount)
        assertEquals(0, metric.commentCount)
    }

    @Test
    fun `카운트는 0보다 작아질 수 없다`() {
        val metric = BootcampMetric(bootcampId = 1L)

        assertThrows(IllegalStateException::class.java) { metric.decreaseViewCount() }
        assertThrows(IllegalStateException::class.java) { metric.decreaseBookmarkCount() }
        assertThrows(IllegalStateException::class.java) { metric.decreaseCommentCount() }
    }
}
