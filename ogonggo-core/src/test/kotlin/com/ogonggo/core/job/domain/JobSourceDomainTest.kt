package com.ogonggo.core.job.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class JobSourceDomainTest {

    @Test
    fun `원문을 다시 지워도 처음 삭제 일시를 지킨다`() {
        // given
        val source = JobSource(jobId = 1L, html = "<p>원문</p>")
        source.delete(DELETED_AT)

        // when
        source.delete(DELETED_AT.plusDays(1))

        // then
        assertEquals(DELETED_AT, source.deletedAt)
    }

    @Test
    fun `삭제한 원문을 다시 저장하면 되살린다`() {
        // given
        val source = JobSource(jobId = 1L, html = "<p>원문</p>")
        source.delete(DELETED_AT)

        // when
        source.replace("<p>새 원문</p>")

        // then
        assertNull(source.deletedAt)
        assertEquals("<p>새 원문</p>", source.html)
    }

    companion object {
        private val DELETED_AT: LocalDateTime = LocalDateTime.of(2026, 11, 12, 10, 0)
    }
}
