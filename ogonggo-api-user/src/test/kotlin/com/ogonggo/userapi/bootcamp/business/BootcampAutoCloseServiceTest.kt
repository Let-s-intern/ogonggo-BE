package com.ogonggo.userapi.bootcamp.business

import com.ogonggo.core.bootcamp.implement.BootcampManager
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class BootcampAutoCloseServiceTest {

    private val manager = Mockito.mock(BootcampManager::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val service = BootcampAutoCloseService(manager, clock)

    @Test
    fun `현재 서울 시각 기준으로 만료 부트캠프 자동 마감을 요청한다`() {
        val now = LocalDateTime.of(2026, 10, 1, 9, 0)
        Mockito.`when`(manager.closeExpired(now)).thenReturn(2)

        service.closeExpired()

        Mockito.verify(manager).closeExpired(now)
    }
}
