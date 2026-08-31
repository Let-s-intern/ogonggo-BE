package com.ogonggo.userapi.bootcamp.implement

import com.ogonggo.core.bootcamp.implement.BootcampMetricManager
import com.ogonggo.userapi.bootcamp.business.BootcampBookmarkChangedEvent
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class BootcampBookmarkChangedEventListenerTest {

    private val bootcampMetricManager = Mockito.mock(BootcampMetricManager::class.java)
    private val listener = BootcampBookmarkChangedEventListener(bootcampMetricManager)

    @Test
    fun `북마크 변경 이벤트를 받으면 북마크 수를 다시 센다`() {
        listener.handle(BootcampBookmarkChangedEvent(1L))

        Mockito.verify(bootcampMetricManager).syncBookmarkCount(1L)
    }

    @Test
    fun `지표 갱신이 실패해도 예외를 전파하지 않는다`() {
        Mockito.doThrow(IllegalStateException("지표 저장 실패"))
            .`when`(bootcampMetricManager).syncBookmarkCount(1L)

        assertDoesNotThrow { listener.handle(BootcampBookmarkChangedEvent(1L)) }
    }
}
