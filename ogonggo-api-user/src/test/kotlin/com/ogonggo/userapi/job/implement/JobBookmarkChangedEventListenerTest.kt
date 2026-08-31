package com.ogonggo.userapi.job.implement

import com.ogonggo.core.job.implement.JobMetricManager
import com.ogonggo.userapi.job.business.JobBookmarkChangedEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class JobBookmarkChangedEventListenerTest {

    private val jobMetricManager = Mockito.mock(JobMetricManager::class.java)
    private val listener = JobBookmarkChangedEventListener(jobMetricManager)

    @Test
    fun `북마크 변경 이벤트를 받으면 북마크 수를 다시 센다`() {
        listener.handle(JobBookmarkChangedEvent(1L))

        Mockito.verify(jobMetricManager).syncBookmarkCount(1L)
    }

    @Test
    fun `지표 갱신이 실패해도 예외를 전파하지 않는다`() {
        Mockito.doThrow(IllegalStateException("지표 저장 실패"))
            .`when`(jobMetricManager).syncBookmarkCount(1L)

        assertDoesNotThrow { listener.handle(JobBookmarkChangedEvent(1L)) }
    }
}
