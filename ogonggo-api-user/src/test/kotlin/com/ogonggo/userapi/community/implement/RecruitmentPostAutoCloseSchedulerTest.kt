package com.ogonggo.userapi.community.implement

import com.ogonggo.core.community.implement.RecruitmentPostManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecruitmentPostAutoCloseSchedulerTest {

    private val manager = Mockito.mock(RecruitmentPostManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZONE)
    private val scheduler = RecruitmentPostAutoCloseScheduler(manager, clock)

    @Test
    fun `현재 날짜 기준으로 만료 모집글 자동 마감을 요청한다`() {
        val today = LocalDate.of(2026, 10, 1)
        val closedAt = LocalDateTime.of(2026, 10, 1, 9, 0)
        Mockito.`when`(manager.closeExpired(today, closedAt)).thenReturn(2)

        scheduler.closeExpiredRecruitmentPosts()

        Mockito.verify(manager).closeExpired(today, closedAt)
        assertEquals(today, LocalDate.now(clock))
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
