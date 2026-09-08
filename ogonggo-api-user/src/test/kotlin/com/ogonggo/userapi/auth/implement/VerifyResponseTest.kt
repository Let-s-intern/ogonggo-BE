package com.ogonggo.userapi.auth.implement

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class VerifyResponseTest {

    @Test
    fun `렛츠커리어가 관리자 여부를 주지 않으면 관리자가 아닌 것으로 본다`() {
        val result = response(isAdmin = null).toResult()

        assertEquals(false, result.isAdmin)
    }

    @Test
    fun `렛츠커리어가 준 관리자 여부를 그대로 옮긴다`() {
        assertEquals(true, response(isAdmin = true).toResult().isAdmin)
        assertEquals(false, response(isAdmin = false).toResult().isAdmin)
    }

    @Test
    fun `연동에 필요한 값을 빠뜨리지 않고 옮긴다`() {
        val result = response(isAdmin = false).toResult()

        assertEquals(4821L, result.userId)
        assertEquals("lets@career.co.kr", result.email)
        assertEquals("김렛츠", result.name)
        assertEquals("렛츠", result.nickname)
        assertEquals("https://example.com/me.png", result.profileImageUrl)
        assertEquals(UPDATED_AT, result.updatedAt)
    }

    private fun response(isAdmin: Boolean?): VerifyResponse = VerifyResponse(
        userId = 4821L,
        email = "lets@career.co.kr",
        name = "김렛츠",
        nickname = "렛츠",
        profileImageUrl = "https://example.com/me.png",
        isAdmin = isAdmin,
        updatedAt = UPDATED_AT,
    )

    companion object {
        private val UPDATED_AT: LocalDateTime = LocalDateTime.of(2026, 9, 1, 10, 0)
    }
}
