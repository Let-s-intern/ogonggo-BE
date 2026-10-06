package com.ogonggo.adminapi.ingestion.work24.implement

import com.ogonggo.adminapi.ingestion.work24.error.Work24ErrorCode
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CollectDto
import com.ogonggo.adminapi.ingestion.work24.implement.dto.Work24CollectionResultDto
import com.ogonggo.core.error.InternalServerException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Work24CollectionSchedulerTest {

    private val collector = Mockito.mock(Work24Collector::class.java)
    private val scheduler = Work24CollectionScheduler(
        collector,
        Clock.fixed(Instant.parse("2026-09-26T19:00:00Z"), ZoneId.of("Asia/Seoul")),
    )

    @Test
    fun `준비되지 않은 대상은 건너뛰고 한 대상이 실패해도 나머지를 서울 시각으로 받는다`() {
        // given
        val targets = Work24CollectionTarget.entries
        targets.forEach { target ->
            Mockito.`when`(collector.isReady(target)).thenReturn(target != Work24CollectionTarget.RECRUITMENTS)
            Mockito.`when`(collector.collect(target, SEOUL_NOW))
                .thenReturn(Work24CollectDto(target, pageCount = 1, appendedCount = 1, skippedCount = 0, excludedCount = 0, failedCount = 0))
        }
        Mockito.`when`(collector.collect(Work24CollectionTarget.WORK_STUDY_COURSES, SEOUL_NOW))
            .thenThrow(InternalServerException(Work24ErrorCode.WORK24_UNAVAILABLE))

        // when
        val results = scheduler.collectAll()

        // then
        assertEquals(targets, results.map { it.target })
        assertEquals(
            Work24CollectionResultDto.Skipped(Work24CollectionTarget.RECRUITMENTS),
            results.first { it.target == Work24CollectionTarget.RECRUITMENTS },
        )
        assertEquals(
            Work24CollectionResultDto.Failed(Work24CollectionTarget.WORK_STUDY_COURSES),
            results.first { it.target == Work24CollectionTarget.WORK_STUDY_COURSES },
        )
        assertEquals(targets.size - 2, results.count { it is Work24CollectionResultDto.Collected })
        Mockito.verify(collector, Mockito.never()).collect(Work24CollectionTarget.RECRUITMENTS, SEOUL_NOW)
    }

    private companion object {
        /** UTC로는 아직 26일이지만 서울은 27일 새벽 4시다. */
        val SEOUL_NOW: LocalDateTime = LocalDateTime.of(2026, 9, 27, 4, 0)
    }
}
