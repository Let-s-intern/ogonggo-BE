package com.ogonggo.userapi.health

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.HttpStatus

class UserHealthControllerTest {

    private val healthEndpoint = Mockito.mock(HealthEndpoint::class.java)
    private val controller = UserHealthController(healthEndpoint)

    @Test
    fun `DB와 Redis에 모두 붙으면 200을 준다`() {
        Mockito.`when`(healthEndpoint.healthForPath("db")).thenReturn(Health.up().build())
        Mockito.`when`(healthEndpoint.healthForPath("redis")).thenReturn(Health.up().build())

        val response = controller.health()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("UP", response.body?.get("status"))
        assertEquals(mapOf("db" to "UP", "redis" to "UP"), response.body?.get("components"))
    }

    @Test
    fun `하나라도 붙지 못하면 503을 준다`() {
        Mockito.`when`(healthEndpoint.healthForPath("db")).thenReturn(Health.up().build())
        Mockito.`when`(healthEndpoint.healthForPath("redis")).thenReturn(Health.down().build())

        val response = controller.health()

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals("DOWN", response.body?.get("status"))
        assertEquals(mapOf("db" to "UP", "redis" to "DOWN"), response.body?.get("components"))
    }

    @Test
    fun `헬스 항목이 등록되지 않았으면 확인할 수 없으므로 503을 준다`() {
        Mockito.`when`(healthEndpoint.healthForPath("db")).thenReturn(Health.up().build())

        val response = controller.health()

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.statusCode)
        assertEquals(mapOf("db" to "UP", "redis" to "DOWN"), response.body?.get("components"))
    }
}
