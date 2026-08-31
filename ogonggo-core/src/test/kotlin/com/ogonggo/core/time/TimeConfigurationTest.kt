package com.ogonggo.core.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class TimeConfigurationTest {

    @Test
    fun `서비스 Clock은 서울 시간대를 사용한다`() {
        val clock = TimeConfiguration().serviceClock()

        assertEquals(ZoneId.of("Asia/Seoul"), clock.zone)
    }
}
