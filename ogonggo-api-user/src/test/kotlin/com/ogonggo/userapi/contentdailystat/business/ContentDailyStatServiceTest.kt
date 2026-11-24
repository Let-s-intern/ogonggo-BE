package com.ogonggo.userapi.contentdailystat.business

import com.ogonggo.core.contentdailystat.implement.ContentDailyStatManager
import com.ogonggo.core.contentevent.implement.ContentEventRemover
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ContentDailyStatServiceTest {

    private val manager = Mockito.mock(ContentDailyStatManager::class.java)
    private val remover = Mockito.mock(ContentEventRemover::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-11-24T18:20:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = ContentDailyStatService(manager, remover, clock)

    @Test
    fun `이틀 전까지 집계된 상태면 그저께 다음 날부터 어제까지 두 날을 집계한다`() {
        // given
        Mockito.`when`(manager.lastStatDate()).thenReturn(YESTERDAY.minusDays(2))

        // when
        val dates = service.datesToAggregate()

        // then
        assertEquals(listOf(YESTERDAY.minusDays(1), YESTERDAY), dates)
    }

    @Test
    fun `집계가 처음이면 어제 하루만 집계한다`() {
        // given
        Mockito.`when`(manager.lastStatDate()).thenReturn(null)

        // when
        val dates = service.datesToAggregate()

        // then
        assertEquals(listOf(YESTERDAY), dates)
    }

    @Test
    fun `어제까지 이미 집계됐어도 어제를 다시 집계한다`() {
        // given
        Mockito.`when`(manager.lastStatDate()).thenReturn(YESTERDAY)

        // when
        val dates = service.datesToAggregate()

        // then
        assertEquals(listOf(YESTERDAY), dates)
    }

    @Test
    fun `오래 멈췄어도 어제부터 31일까지만 거슬러 올라간다`() {
        // given
        Mockito.`when`(manager.lastStatDate()).thenReturn(YESTERDAY.minusDays(100))

        // when
        val dates = service.datesToAggregate()

        // then
        assertEquals(31, dates.size)
        assertEquals(YESTERDAY.minusDays(30), dates.first())
        assertEquals(YESTERDAY, dates.last())
    }

    @Test
    fun `마지막 집계일이 어제로부터 31일 전이면 31일을 모두 집계한다`() {
        // given
        Mockito.`when`(manager.lastStatDate()).thenReturn(YESTERDAY.minusDays(31))

        // when
        val dates = service.datesToAggregate()

        // then
        assertEquals(31, dates.size)
        assertEquals(YESTERDAY.minusDays(30), dates.first())
    }

    private companion object {
        /** 시계는 2026-11-25 03:20(KST)이다. */
        val YESTERDAY: LocalDate = LocalDate.of(2026, 11, 24)
    }
}
