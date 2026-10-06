package com.ogonggo.userapi.recruitmentpost.implement

import com.ogonggo.core.recruitmentpost.implement.RecruitmentPostMetricManager
import com.ogonggo.userapi.recruitmentpost.business.RecruitmentPostViewedEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RecruitmentPostMetricEventListenerTest {

    private val recruitmentPostMetricManager = Mockito.mock(RecruitmentPostMetricManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-12T01:00:00Z"), ZONE)

    @Test
    fun `모집글 조회 이벤트를 받으면 조회 수를 증가시킨다`() {
        val listener = RecruitmentPostViewedEventListener(recruitmentPostMetricManager, clock)

        listener.handle(RecruitmentPostViewedEvent(POST_ID))

        Mockito.verify(recruitmentPostMetricManager).increaseViewCount(POST_ID, LocalDateTime.now(clock))
    }

    @Test
    fun `지표 기록이 실패해도 예외를 전파하지 않는다`() {
        val listener = RecruitmentPostViewedEventListener(recruitmentPostMetricManager, clock)
        Mockito.doThrow(IllegalStateException("지표 저장 실패"))
            .`when`(recruitmentPostMetricManager).increaseViewCount(POST_ID, LocalDateTime.now(clock))

        assertDoesNotThrow { listener.handle(RecruitmentPostViewedEvent(POST_ID)) }
    }

    companion object {
        private const val POST_ID = 12L
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
