package com.ogonggo.adminapi.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.HttpStatus

class AdminHealthControllerTest {

    private val healthEndpoint = Mockito.mock(HealthEndpoint::class.java)
    private val controller = AdminHealthController(healthEndpoint)

    @Test
    fun `DB에 붙지 못하면 503을 준다`() {
        Mockito.`when`(healthEndpoint.healthForPath("db")).thenReturn(Health.down().build())

        val response = controller.health()

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals("DOWN", response.body?.get("status"))
        assertEquals(mapOf("db" to "DOWN"), response.body?.get("components"))
    }

    @Test
    fun `헬스 항목이 등록되지 않았으면 확인할 수 없으므로 503을 준다`() {
        val response = controller.health()

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
    }
}
