package com.ogonggo.core.community.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RecruitmentPostApplicationDomainTest {

    @Test
    fun `모집글과 사용자 식별자는 양수여야 한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecruitmentPostApplication(
                postId = 0L,
                userId = USER_ID,
                firstClickedAt = FIRST,
                lastClickedAt = FIRST,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            RecruitmentPostApplication(
                postId = POST_ID,
                userId = 0L,
                firstClickedAt = FIRST,
                lastClickedAt = FIRST,
            )
        }
    }

    @Test
    fun `재접근해도 최초 시각은 유지하고 최근 시각만 갱신한다`() {
        val application = RecruitmentPostApplication(
            postId = POST_ID,
            userId = USER_ID,
            firstClickedAt = FIRST,
            lastClickedAt = FIRST,
        )

        application.recordClick(SECOND)
        application.recordClick(FIRST.minusMinutes(1))

        assertEquals(FIRST, application.firstClickedAt)
        assertEquals(SECOND, application.lastClickedAt)
    }

    companion object {
        private const val POST_ID = 12L
        private const val USER_ID = 17L
        private val FIRST = LocalDateTime.of(2026, 9, 16, 9, 0)
        private val SECOND = FIRST.plusMinutes(10)
    }
}
